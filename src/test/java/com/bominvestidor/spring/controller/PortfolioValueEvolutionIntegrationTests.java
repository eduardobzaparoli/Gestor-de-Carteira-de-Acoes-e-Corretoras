package com.bominvestidor.spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetKey;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPrice;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventStatus;
import com.bominvestidor.spring.domain.income.IncomeEventType;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.income.PortfolioIncomeEventEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategy;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;
import com.bominvestidor.spring.integration.exchange.ExchangeRateStrategy;
import com.bominvestidor.spring.integration.historicalprice.HistoricalAssetPriceStrategy;
import com.bominvestidor.spring.integration.historicalprice.HistoricalAssetPriceStrategyResolver;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.income.PortfolioIncomeEventRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PortfolioValueEvolutionIntegrationTests.StubConfiguration.class)
class PortfolioValueEvolutionIntegrationTests {
	private static final Instant NOW = Instant.parse("2026-09-03T12:00:00Z");
	@Autowired private MockMvc mockMvc;
	@Autowired private AuthService auth;
	@Autowired private UserRepository users;
	@Autowired private BrokerageRepository brokerages;
	@Autowired private PortfolioRepository portfolios;
	@Autowired private PortfolioTransactionRepository transactions;
	@Autowired private PortfolioIncomeEventRepository incomeEvents;

	@Test
	void calculatesBrazilianHistoryWithCostsPartialSaleAndCurrentPointOnH2() throws Exception {
		Session session = session();
		PortfolioEntity portfolio = portfolio(session.user());
		transaction(portfolio, "PETR4", AssetMarket.BR, "BRL", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 24), "10", "10", "2", 1);
		transaction(portfolio, "PETR4", AssetMarket.BR, "BRL", TransactionType.SELL, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 28), "4", "99", "7", 2);

		mockMvc.perform(get(path(portfolio)).with(investor(session)))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].date").value("2026-08-24"))
				.andExpect(jsonPath("$[0].investedValue").value(102))
				.andExpect(jsonPath("$[0].marketValue").value(420))
				.andExpect(jsonPath("$[2].date").value("2026-08-28"))
				.andExpect(jsonPath("$[2].investedValue").value(61.2))
				.andExpect(jsonPath("$[2].marketValue").value(264))
				.andExpect(jsonPath("$[4].date").value("2026-09-03"))
				.andExpect(jsonPath("$[4].marketValue").value(270));
	}

	@Test
	void consolidatesMixedCalendarsHistoricalPtaxAndCurrentQuotesEntirelyInBrl() throws Exception {
		Session session = session();
		PortfolioEntity portfolio = portfolio(session.user());
		transaction(portfolio, "PETR4", AssetMarket.BR, "BRL", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 24), "2", "40", "1", 1);
		transaction(portfolio, "MSFT", AssetMarket.US, "USD", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 27), "1", "400", "1", 2);

		mockMvc.perform(get(path(portfolio)).with(investor(session)))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].date").value("2026-08-24"))
				.andExpect(jsonPath("$[0].investedValue").value(81))
				.andExpect(jsonPath("$[1].date").value("2026-08-27"))
				.andExpect(jsonPath("$[1].investedValue").value(2086))
				.andExpect(jsonPath("$[1].marketValue").value(2586))
				.andExpect(jsonPath("$[2].date").value("2026-08-28"))
				.andExpect(jsonPath("$[2].marketValue").value(2638))
				.andExpect(jsonPath("$[3].date").value("2026-08-31"))
				.andExpect(jsonPath("$[3].marketValue").value(2740))
				.andExpect(jsonPath("$[5].date").value("2026-09-03"))
				.andExpect(jsonPath("$[5].marketValue").value(2898));
	}

	@Test
	void totalSaleLeavesNoCashAndIncomePendingAndCancelledEntriesDoNotChangeTheSeries() throws Exception {
		Session session = session();
		PortfolioEntity portfolio = portfolio(session.user());
		transaction(portfolio, "PETR4", AssetMarket.BR, "BRL", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 24), "10", "10", "0", 1);
		transaction(portfolio, "PETR4", AssetMarket.BR, "BRL", TransactionType.SELL, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 27), "10", "99", "0", 2);
		transaction(portfolio, "MSFT", AssetMarket.US, "USD", TransactionType.BUY, TransactionStatus.PENDING,
				LocalDate.of(2026, 9, 4), "1", "400", "0", 3);
		transaction(portfolio, "VALE3", AssetMarket.BR, "BRL", TransactionType.BUY, TransactionStatus.CANCELLED,
				LocalDate.of(2026, 8, 25), "5", "50", "0", 4);
		incomeEvents.saveAndFlush(new PortfolioIncomeEventEntity(UUID.randomUUID(), portfolio, "PETR4", "PETR4",
				AssetMarket.BR, AssetType.STOCK, "BRL", IncomeEventType.DIVIDEND, IncomeEventSource.MANUAL,
				"manual:" + UUID.randomUUID(), IncomeEventStatus.EFFECTIVE, LocalDate.of(2026, 8, 24),
				LocalDate.of(2026, 8, 28), new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("10"),
				new BigDecimal("10"), null, "ignored by evolution", NOW, NOW));

		mockMvc.perform(get(path(portfolio)).with(investor(session)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].date").value("2026-08-24"))
				.andExpect(jsonPath("$[0].investedValue").value(100))
				.andExpect(jsonPath("$[0].marketValue").value(420));
	}

	@Test
	void returnsSanitizedAtomicErrorsForTruncatedCoverageMissingExchangeAndProviderLimit() throws Exception {
		Session session = session();

		PortfolioEntity truncated = portfolio(session.user());
		transaction(truncated, "PETR4", AssetMarket.BR, "BRL", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 24), "1", "40", "0", 1);
		transaction(truncated, "TRUNC", AssetMarket.US, "USD", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 27), "1", "10", "0", 2);
		mockMvc.perform(get(path(truncated)).with(investor(session)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("HISTORICAL_PRICE_UNAVAILABLE"))
				.andExpect(jsonPath("$.points").doesNotExist());

		PortfolioEntity noExchange = portfolio(session.user());
		transaction(noExchange, "MSFT", AssetMarket.US, "USD", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 26), "1", "400", "1", 3);
		mockMvc.perform(get(path(noExchange)).with(investor(session)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("EXCHANGE_RATE_UNAVAILABLE"))
				.andExpect(jsonPath("$.points").doesNotExist());

		PortfolioEntity limited = portfolio(session.user());
		transaction(limited, "LIMIT", AssetMarket.US, "USD", TransactionType.BUY, TransactionStatus.EFFECTIVE,
				LocalDate.of(2026, 8, 27), "1", "400", "1", 4);
		mockMvc.perform(get(path(limited)).with(investor(session)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("TWELVE_DATA_RATE_LIMITED"))
				.andExpect(jsonPath("$.message").value("Twelve Data rate limit reached"))
				.andExpect(jsonPath("$.providerPayload").doesNotExist());
	}

	private String path(PortfolioEntity portfolio) { return "/api/portfolios/" + portfolio.getId() + "/value-evolution"; }
	private org.springframework.test.web.servlet.request.RequestPostProcessor investor(Session session) {
		return jwt().jwt(token -> token.subject(session.user().getId().toString()).claim("role", "INVESTOR"))
				.authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_INVESTOR"));
	}

	private Session session() {
		String email = UUID.randomUUID() + "@example.com";
		auth.register(new RegisterRequest("Evolution Investor", email, "password123"));
		return new Session(users.findByEmail(email).orElseThrow());
	}

	private PortfolioEntity portfolio(UserEntity owner) {
		String cnpj = "%014d".formatted(Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000_000_000L));
		BrokerageEntity brokerage = brokerages.saveAndFlush(new BrokerageEntity(UUID.randomUUID(), owner, "Broker " + UUID.randomUUID(),
				UUID.randomUUID().toString(), cnpj, "Legal", null, "ACTIVE", "BROKERS", "01445000",
				"Rua", "Bairro", "1", null, "São Paulo", "SP", NOW, NOW));
		return portfolios.saveAndFlush(new PortfolioEntity(UUID.randomUUID(), owner, brokerage, "Portfolio " + UUID.randomUUID(),
				UUID.randomUUID().toString(), NOW, NOW));
	}

	private void transaction(PortfolioEntity portfolio, String ticker, AssetMarket market, String currency, TransactionType type,
			TransactionStatus status, LocalDate date, String quantity, String price, String costs, int order) {
		Instant createdAt = NOW.plusSeconds(order);
		transactions.saveAndFlush(new PortfolioTransactionEntity(UUID.randomUUID(), portfolio, ticker, ticker, market, AssetType.STOCK,
				currency, type, status, date, new BigDecimal(quantity), new BigDecimal(price), new BigDecimal(costs), createdAt, createdAt));
	}

	private record Session(UserEntity user) { }

	@TestConfiguration(proxyBeanMethods = false)
	static class StubConfiguration {
		@Bean @Primary Clock evolutionClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }

		@Bean @Primary HistoricalAssetPriceStrategyResolver historicalResolver() {
			return new HistoricalAssetPriceStrategyResolver(List.of(new HistoricalStub(AssetMarket.BR), new HistoricalStub(AssetMarket.US)));
		}

		@Bean @Primary AssetSearchStrategyResolver assetResolver() {
			return new AssetSearchStrategyResolver(List.of(new QuoteStub(AssetMarket.BR), new QuoteStub(AssetMarket.US)));
		}

		@Bean @Primary ExchangeRateStrategy exchangeRateStrategy() {
			return (source, target, date) -> {
				if (date.equals(LocalDate.of(2026, 8, 26))) return Optional.empty();
				BigDecimal rate = switch (date.toString()) {
					case "2026-08-27" -> new BigDecimal("5.0");
					case "2026-08-28" -> new BigDecimal("5.1");
					case "2026-08-31" -> new BigDecimal("5.2");
					case "2026-09-01" -> new BigDecimal("5.25");
					case "2026-09-02" -> new BigDecimal("5.3");
					default -> new BigDecimal("5.4");
				};
				return Optional.of(new ExchangeRate(source, target, rate, date));
			};
		}
	}

	private static final class HistoricalStub implements HistoricalAssetPriceStrategy {
		private final AssetMarket market;
		private HistoricalStub(AssetMarket market) { this.market = market; }
		@Override public AssetMarket market() { return market; }
		@Override public HistoricalAssetPriceSeries findSeries(String ticker, String currency, LocalDate startDate, LocalDate endDate) {
			if ("LIMIT".equals(ticker)) throw new AssetProviderUnavailableException("TWELVE_DATA_RATE_LIMITED", "Twelve Data rate limit reached");
			NavigableMap<LocalDate, HistoricalAssetPrice> prices = new TreeMap<>();
			if ("TRUNC".equals(ticker)) {
				put(prices, "2026-08-28", "10");
			} else if (market == AssetMarket.BR) {
				put(prices, "2026-08-24", "42"); put(prices, "2026-08-27", "43");
				put(prices, "2026-08-28", "44"); put(prices, "2026-09-02", "47");
			} else {
				put(prices, "2026-08-27", "500"); put(prices, "2026-08-31", "510"); put(prices, "2026-09-02", "515");
			}
			return new HistoricalAssetPriceSeries(new HistoricalAssetKey(market, ticker, currency), prices);
		}
		private void put(NavigableMap<LocalDate, HistoricalAssetPrice> prices, String date, String close) {
			LocalDate parsed = LocalDate.parse(date);
			prices.put(parsed, new HistoricalAssetPrice(parsed, new BigDecimal(close)));
		}
	}

	private static final class QuoteStub implements AssetSearchStrategy {
		private final AssetMarket market;
		private QuoteStub(AssetMarket market) { this.market = market; }
		@Override public AssetMarket market() { return market; }
		@Override public List<AssetCandidate> findCandidates(AssetType type, String query) { return List.of(); }
		@Override public Optional<AssetQuote> findQuote(String ticker) {
			BigDecimal price = market == AssetMarket.BR ? new BigDecimal("45") : new BigDecimal("520");
			return Optional.of(new AssetQuote(ticker, market == AssetMarket.BR ? "BRL" : "USD", price));
		}
	}
}
