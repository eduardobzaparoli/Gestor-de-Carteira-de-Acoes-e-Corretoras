package com.bominvestidor.spring.service.income;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventStatus;
import com.bominvestidor.spring.domain.income.IncomeEventType;
import com.bominvestidor.spring.dto.income.PortfolioIncomeSummaryResponse;
import com.bominvestidor.spring.entity.income.PortfolioIncomeEventEntity;
import com.bominvestidor.spring.exception.ExchangeRateUnavailableException;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategyResolver;
import com.bominvestidor.spring.mapper.income.PortfolioIncomeEventMapper;
import com.bominvestidor.spring.repository.income.PortfolioIncomeEventRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.exchange.ExchangeRateService;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

class PortfolioIncomeEventServiceTests {
	private final UUID ownerId = UUID.randomUUID();
	private final UUID portfolioId = UUID.randomUUID();
	private PortfolioIncomeEventRepository repository;
	private ExchangeRateService exchangeRateService;
	private PortfolioIncomeEventService service;

	@BeforeEach
	void setUp() {
		PortfolioService portfolioService = mock(PortfolioService.class);
		repository = mock(PortfolioIncomeEventRepository.class);
		exchangeRateService = mock(ExchangeRateService.class);
		Clock clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
		service = new PortfolioIncomeEventService(portfolioService, mock(PortfolioTransactionRepository.class), repository,
				mock(IncomeEventProviderStrategyResolver.class), new IncomeEventCandidateCache(clock, new BrokerageIntegrationProperties()),
				new PortfolioIncomeEventMapper(), mock(PortfolioIncomeEventReconciliationService.class), exchangeRateService, clock);
	}

	@Test
	void returnsEmptySummaryWithoutConsultingExchangeProvider() {
		when(repository.findAllByPortfolio_IdAndStatus(portfolioId, IncomeEventStatus.EFFECTIVE)).thenReturn(List.of());
		PortfolioIncomeSummaryResponse response = service.summary(ownerId, portfolioId);
		assertThat(response.currencySummaries()).isEmpty();
		assertThat(response.consolidatedReceivedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
		verify(exchangeRateService, never()).find(any(), any(), any());
	}

	@Test
	void sumsBrazilianIncomeWithoutConsultingExchangeProvider() {
		when(repository.findAllByPortfolio_IdAndStatus(portfolioId, IncomeEventStatus.EFFECTIVE)).thenReturn(List.of(event("BRL", "12.50", "2026-05-20")));
		PortfolioIncomeSummaryResponse response = service.summary(ownerId, portfolioId);
		assertThat(response.currencySummaries()).singleElement().satisfies(item -> assertThat(item.receivedAmount()).isEqualByComparingTo("12.50"));
		assertThat(response.consolidatedReceivedAmount()).isEqualByComparingTo("12.50");
		verify(exchangeRateService, never()).find(any(), any(), any());
	}

	@Test
	void convertsDollarIncomeUsingHistoricalRate() {
		when(repository.findAllByPortfolio_IdAndStatus(portfolioId, IncomeEventStatus.EFFECTIVE)).thenReturn(List.of(event("USD", "10", "2026-05-20")));
		when(exchangeRateService.find("USD", "BRL", LocalDate.parse("2026-05-20"))).thenReturn(Optional.of(new ExchangeRate("USD", "BRL", new BigDecimal("5.20"), LocalDate.parse("2026-05-19"))));
		PortfolioIncomeSummaryResponse response = service.summary(ownerId, portfolioId);
		assertThat(response.consolidatedReceivedAmount()).isEqualByComparingTo("52.00");
		assertThat(response.exchangeRates()).singleElement().satisfies(rate -> assertThat(rate.referenceDate()).isEqualTo(LocalDate.parse("2026-05-19")));
	}

	@Test
	void combinesCurrenciesAndUsesEachPaymentDateRate() {
		when(repository.findAllByPortfolio_IdAndStatus(portfolioId, IncomeEventStatus.EFFECTIVE)).thenReturn(List.of(
				event("BRL", "10", "2026-05-20"), event("USD", "2", "2026-05-20"), event("USD", "3", "2026-05-21")));
		when(exchangeRateService.find("USD", "BRL", LocalDate.parse("2026-05-20"))).thenReturn(Optional.of(new ExchangeRate("USD", "BRL", new BigDecimal("5"), LocalDate.parse("2026-05-19"))));
		when(exchangeRateService.find("USD", "BRL", LocalDate.parse("2026-05-21"))).thenReturn(Optional.of(new ExchangeRate("USD", "BRL", new BigDecimal("6"), LocalDate.parse("2026-05-21"))));
		PortfolioIncomeSummaryResponse response = service.summary(ownerId, portfolioId);
		assertThat(response.currencySummaries()).hasSize(2);
		assertThat(response.consolidatedReceivedAmount()).isEqualByComparingTo("38");
		assertThat(response.exchangeRates()).hasSize(2);
	}

	@Test
	void failsAtomicallyWhenDollarRateIsUnavailable() {
		when(repository.findAllByPortfolio_IdAndStatus(portfolioId, IncomeEventStatus.EFFECTIVE)).thenReturn(List.of(event("USD", "10", "2026-05-20")));
		when(exchangeRateService.find(eq("USD"), eq("BRL"), any())).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.summary(ownerId, portfolioId)).isInstanceOf(ExchangeRateUnavailableException.class);
	}

	private PortfolioIncomeEventEntity event(String currency, String amount, String paymentDate) {
		Instant now = Instant.parse("2026-05-01T00:00:00Z");
		return new PortfolioIncomeEventEntity(UUID.randomUUID(), null, "PETR4", "Petrobras", AssetMarket.BR, AssetType.STOCK, currency,
				IncomeEventType.DIVIDEND, IncomeEventSource.MANUAL, UUID.randomUUID().toString(), IncomeEventStatus.EFFECTIVE, LocalDate.parse("2026-05-10"),
				LocalDate.parse(paymentDate), null, null, null, new BigDecimal(amount), null, null, now, now);
	}
}
