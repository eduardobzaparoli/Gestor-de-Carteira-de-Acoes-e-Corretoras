package com.bominvestidor.spring.service.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetKey;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPrice;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.exception.HistoricalPriceUnavailableException;
import com.bominvestidor.spring.exception.PortfolioNotFoundException;
import com.bominvestidor.spring.mapper.evolution.PortfolioValueEvolutionMapper;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.asset.AssetSearchService;
import com.bominvestidor.spring.service.exchange.ExchangeRateService;
import com.bominvestidor.spring.service.portfolio.PortfolioService;
import com.bominvestidor.spring.service.transaction.PortfolioTransactionReconciliationService;

@ExtendWith(MockitoExtension.class)
class PortfolioValueEvolutionServiceTests {
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 3);
	private static final LocalDate START = LocalDate.of(2026, 6, 6);
	@Mock private PortfolioService portfolios;
	@Mock private PortfolioTransactionReconciliationService reconciliation;
	@Mock private PortfolioTransactionRepository transactions;
	@Mock private HistoricalAssetPriceService historicalPrices;
	@Mock private AssetSearchService assets;
	@Mock private ExchangeRateService exchangeRates;
	private final UUID ownerId = UUID.randomUUID();
	private final UUID portfolioId = UUID.randomUUID();

	@Test
	void returnsEmptyWithoutExternalCallsWhenThereWasNoCustodyInTheWindow() {
		PortfolioTransactionEntity oldBuy = transaction("PETR4", AssetMarket.BR, "BRL", TransactionType.BUY,
				TransactionStatus.EFFECTIVE, LocalDate.of(2026, 5, 1), "2", "40", "1", 1);
		PortfolioTransactionEntity oldSale = transaction("PETR4", AssetMarket.BR, "BRL", TransactionType.SELL,
				TransactionStatus.EFFECTIVE, LocalDate.of(2026, 5, 2), "2", "50", "0", 2);
		when(transactions.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId)).thenReturn(List.of(oldBuy, oldSale));

		assertEquals(List.of(), service().find(ownerId, portfolioId));

		verify(portfolios).ownedPortfolio(ownerId, portfolioId);
		verify(reconciliation).reconcile(portfolioId);
		verifyNoInteractions(historicalPrices, assets, exchangeRates);
	}

	@Test
	void consolidatesMixedMarketsWithDifferentCalendarsAndCurrentPointInBrl() {
		PortfolioTransactionEntity petr = transaction("PETR4", AssetMarket.BR, "BRL", TransactionType.BUY,
				TransactionStatus.EFFECTIVE, LocalDate.of(2026, 8, 24), "2", "40", "1", 1);
		PortfolioTransactionEntity msft = transaction("MSFT", AssetMarket.US, "USD", TransactionType.BUY,
				TransactionStatus.EFFECTIVE, LocalDate.of(2026, 8, 27), "1", "400", "1", 2);
		when(transactions.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId)).thenReturn(List.of(petr, msft));
		when(historicalPrices.find(AssetMarket.BR, "PETR4", "BRL", START, TODAY.minusDays(1)))
				.thenReturn(series(AssetMarket.BR, "PETR4", "BRL", Map.of(
						LocalDate.of(2026, 8, 24), "42", LocalDate.of(2026, 8, 27), "43")));
		when(historicalPrices.find(AssetMarket.US, "MSFT", "USD", START, TODAY.minusDays(1)))
				.thenReturn(series(AssetMarket.US, "MSFT", "USD", Map.of(
						LocalDate.of(2026, 8, 27), "500", LocalDate.of(2026, 8, 28), "510")));
		when(assets.findQuote(AssetMarket.BR, "PETR4")).thenReturn(Optional.of(new AssetQuote("PETR4", "BRL", new BigDecimal("50"))));
		when(assets.findQuote(AssetMarket.US, "MSFT")).thenReturn(Optional.of(new AssetQuote("MSFT", "USD", new BigDecimal("520"))));
		when(exchangeRates.find(eq("USD"), eq("BRL"), any(LocalDate.class))).thenAnswer(invocation -> {
			LocalDate date = invocation.getArgument(2);
			return Optional.of(new ExchangeRate("USD", "BRL", new BigDecimal("5"), date));
		});

		var result = service().find(ownerId, portfolioId);

		assertEquals(List.of(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 27), LocalDate.of(2026, 8, 28), TODAY),
				result.stream().map(value -> value.date()).toList());
		assertDecimal("81", result.get(0).investedValue());
		assertDecimal("84", result.get(0).marketValue());
		assertDecimal("2086", result.get(1).investedValue());
		assertDecimal("2586", result.get(1).marketValue());
		assertDecimal("2636", result.get(2).marketValue());
		assertDecimal("2700", result.get(3).marketValue());
		verify(historicalPrices).find(AssetMarket.BR, "PETR4", "BRL", START, TODAY.minusDays(1));
		verify(historicalPrices).find(AssetMarket.US, "MSFT", "USD", START, TODAY.minusDays(1));
	}

	@Test
	void propagatesMissingHistoricalInputAtomicallyBeforeCurrentQuotesAndExchangeRates() {
		PortfolioTransactionEntity petr = transaction("PETR4", AssetMarket.BR, "BRL", TransactionType.BUY,
				TransactionStatus.EFFECTIVE, LocalDate.of(2026, 8, 24), "2", "40", "1", 1);
		when(transactions.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId)).thenReturn(List.of(petr));
		when(historicalPrices.find(AssetMarket.BR, "PETR4", "BRL", START, TODAY.minusDays(1)))
				.thenThrow(new HistoricalPriceUnavailableException());

		assertThrows(HistoricalPriceUnavailableException.class, () -> service().find(ownerId, portfolioId));
		verifyNoInteractions(assets, exchangeRates);
	}

	@Test
	void checksOwnershipBeforeReconciliationRepositoryAndProviders() {
		when(portfolios.ownedPortfolio(ownerId, portfolioId)).thenThrow(new PortfolioNotFoundException());

		assertThrows(PortfolioNotFoundException.class, () -> service().find(ownerId, portfolioId));

		verify(reconciliation, never()).reconcile(any());
		verifyNoInteractions(transactions, historicalPrices, assets, exchangeRates);
	}

	@Test
	void valuesAnAssetBoughtTodayWithoutRequestingHistoricalPrices() {
		PortfolioTransactionEntity boughtToday = transaction("MSFT", AssetMarket.US, "USD", TransactionType.BUY,
				TransactionStatus.EFFECTIVE, TODAY, "1", "100", "1", 1);
		when(transactions.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId)).thenReturn(List.of(boughtToday));
		when(assets.findQuote(AssetMarket.US, "MSFT"))
				.thenReturn(Optional.of(new AssetQuote("MSFT", "USD", new BigDecimal("110"))));
		when(exchangeRates.find("USD", "BRL", TODAY))
				.thenReturn(Optional.of(new ExchangeRate("USD", "BRL", new BigDecimal("5"), TODAY)));

		var result = service().find(ownerId, portfolioId);

		assertEquals(1, result.size());
		assertDecimal("505", result.get(0).investedValue());
		assertDecimal("550", result.get(0).marketValue());
		verifyNoInteractions(historicalPrices);
	}

	private PortfolioValueEvolutionService service() {
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		return new PortfolioValueEvolutionService(portfolios, reconciliation, transactions, historicalPrices, assets,
				exchangeRates, new PortfolioValueEvolutionMapper(),
				Clock.fixed(Instant.parse("2026-09-03T12:00:00Z"), ZoneOffset.UTC), properties);
	}

	private HistoricalAssetPriceSeries series(AssetMarket market, String ticker, String currency, Map<LocalDate, String> values) {
		TreeMap<LocalDate, HistoricalAssetPrice> prices = new TreeMap<>();
		values.forEach((date, close) -> prices.put(date, new HistoricalAssetPrice(date, new BigDecimal(close))));
		return new HistoricalAssetPriceSeries(new HistoricalAssetKey(market, ticker, currency), prices);
	}

	private PortfolioTransactionEntity transaction(String ticker, AssetMarket market, String currency, TransactionType type,
			TransactionStatus status, LocalDate date, String quantity, String price, String costs, int order) {
		Instant createdAt = Instant.parse("2026-05-01T12:00:00Z").plusSeconds(order);
		return new PortfolioTransactionEntity(UUID.randomUUID(), null, ticker, ticker, market, AssetType.STOCK, currency,
				type, status, date, new BigDecimal(quantity), new BigDecimal(price), new BigDecimal(costs), createdAt, createdAt);
	}

	private void assertDecimal(String expected, BigDecimal actual) {
		assertEquals(0, new BigDecimal(expected).compareTo(actual));
	}
}
