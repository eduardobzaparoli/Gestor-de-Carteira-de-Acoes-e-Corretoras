package com.bominvestidor.spring.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bominvestidor.spring.domain.asset.*;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.*;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.integration.asset.*;
import com.bominvestidor.spring.integration.exchange.ExchangeRateStrategy;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AssetSearchIntegrationTests.StubProviders.class)
class AssetSearchIntegrationTests {
	private static final AtomicBoolean EXCHANGE_AVAILABLE = new AtomicBoolean(true);
	private static final AtomicBoolean HISTORICAL_EXCHANGE_AVAILABLE = new AtomicBoolean(true);
	@Autowired MockMvc mockMvc; @Autowired AuthService authService; @Autowired UserRepository users;
	@Autowired BrokerageRepository brokerages; @Autowired PortfolioRepository portfolios;

	@Test void returnsPublicQuotedResultsAndProtectsTheRoute() throws Exception {
		Session first = session(); UUID portfolioId = portfolio(first.user()).getId();
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").param("query", "PETR").header("Authorization", "Bearer " + first.token()))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].ticker").value("PETR4")).andExpect(jsonPath("$[0].price").value(35.10));
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "ETF").param("query", "BOVA").header("Authorization", "Bearer " + first.token()))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].assetType").value("ETF"));
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "US").param("assetType", "STOCK").param("query", "MSFT").header("Authorization", "Bearer " + first.token()))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].assetType").value("STOCK"));
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "US").param("assetType", "ETF").param("query", "SPY").header("Authorization", "Bearer " + first.token()))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].assetType").value("ETF"));
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").param("query", "P"))
			.andExpect(status().isUnauthorized());
		Jwt admin = Jwt.withTokenValue("admin").subject(UUID.randomUUID().toString()).claim("role", UserRole.ADMIN.name()).header("alg", "none").build();
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").param("query", "PETR").with(jwt().jwt(admin)))
			.andExpect(status().isForbidden());
		Session other = session();
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").param("query", "PETR").header("Authorization", "Bearer " + other.token()))
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
	}

	@Test void validatesRequiredParametersAndExposesProviderFailuresWithoutInternalDetails() throws Exception {
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").param("query", "P").header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR")).andExpect(jsonPath("$.fieldErrors[0].field").value("query"));
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "INVALID").param("assetType", "STOCK").param("query", "PETR").header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("market"));
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").param("query", "EMPTY").header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "BR").param("assetType", "STOCK").param("query", "FAIL").header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("BRAPI_PROVIDER_UNAVAILABLE"));
		mockMvc.perform(get("/api/portfolios/{id}/assets", portfolioId).param("market", "US").param("assetType", "STOCK").param("query", "FAIL").header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("ALPHAVANTAGE_RATE_LIMITED"));
	}

	@Test void valuesOpenPositionsByCurrencyAndProtectsTheRoute() throws Exception {
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		transaction(session, portfolioId, "PETR4", "BR", "BRL", "2", "30");
		transaction(session, portfolioId, "MSFT", "US", "USD", "3", "100");

		mockMvc.perform(get("/api/portfolios/{id}/valuation", portfolioId).header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isOk()).andExpect(jsonPath("$.positions.length()").value(2))
			.andExpect(jsonPath("$.positions[0].ticker").value("PETR4"))
			.andExpect(jsonPath("$.positions[0].currentPrice").value(35.10))
			.andExpect(jsonPath("$.positions[0].marketValue").value(70.20))
			.andExpect(jsonPath("$.currencySummaries.length()").value(2))
			.andExpect(jsonPath("$.currencySummaries[0].currency").value("BRL"))
			.andExpect(jsonPath("$.currencySummaries[1].currency").value("USD"))
			.andExpect(jsonPath("$.consolidatedSummary.baseCurrency").value("BRL"))
			.andExpect(jsonPath("$.consolidatedSummary.investedValue").value(1560))
			.andExpect(jsonPath("$.consolidatedSummary.marketValue").value(596.70))
			.andExpect(jsonPath("$.consolidatedSummary.exchangeRates[0].sourceCurrency").value("USD"))
			.andExpect(jsonPath("$.consolidatedSummary.historicalExchangeRates[0].sourceCurrency").value("USD"));
		mockMvc.perform(get("/api/portfolios/{id}/valuation", portfolioId)).andExpect(status().isUnauthorized());
		Session other = session();
		mockMvc.perform(get("/api/portfolios/{id}/valuation", portfolioId).header("Authorization", "Bearer " + other.token()))
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
	}

	@Test
	@org.springframework.test.annotation.DirtiesContext(methodMode = org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD)
	void failsAtomicallyWhenAnHistoricalExchangeRateIsUnavailable() throws Exception {
		HISTORICAL_EXCHANGE_AVAILABLE.set(false);
		try {
			Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
			transaction(session, portfolioId, "MSFT", "US", "USD", "1", "100", LocalDate.now().minusDays(2));
			mockMvc.perform(get("/api/portfolios/{id}/valuation", portfolioId).header("Authorization", "Bearer " + session.token()))
					.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("EXCHANGE_RATE_UNAVAILABLE"))
					.andExpect(jsonPath("$.positions").doesNotExist());
		} finally { HISTORICAL_EXCHANGE_AVAILABLE.set(true); }
	}

	@Test
	@org.springframework.test.annotation.DirtiesContext(methodMode = org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD)
	void failsAtomicallyWhenTheExchangeRateIsUnavailable() throws Exception {
		EXCHANGE_AVAILABLE.set(false);
		try {
			Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
			transaction(session, portfolioId, "MSFT", "US", "USD", "1", "100");
			mockMvc.perform(get("/api/portfolios/{id}/valuation", portfolioId).header("Authorization", "Bearer " + session.token()))
					.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("EXCHANGE_RATE_UNAVAILABLE"))
					.andExpect(jsonPath("$.positions").doesNotExist());
		} finally { EXCHANGE_AVAILABLE.set(true); }
	}

	private void transaction(Session session, UUID portfolioId, String ticker, String market, String currency, String quantity, String price) throws Exception {
		transaction(session, portfolioId, ticker, market, currency, quantity, price, LocalDate.now());
	}
	private void transaction(Session session, UUID portfolioId, String ticker, String market, String currency, String quantity, String price,
			LocalDate transactionDate) throws Exception {
		String body = "{\"ticker\":\"%s\",\"assetName\":\"%s\",\"market\":\"%s\",\"assetType\":\"STOCK\",\"currency\":\"%s\",\"type\":\"BUY\",\"transactionDate\":\"%s\",\"quantity\":%s,\"unitPrice\":%s,\"costs\":0}"
				.formatted(ticker, ticker, market, currency, transactionDate, quantity, price);
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/portfolios/{id}/transactions", portfolioId)
				.header("Authorization", "Bearer " + session.token()).contentType("application/json").content(body)).andExpect(status().isCreated());
	}

	private Session session() { String email = UUID.randomUUID()+"@example.com"; authService.register(new RegisterRequest("Investor",email,"password123")); return new Session(authService.login(new LoginRequest(email,"password123")).token(), users.findByEmail(email).orElseThrow()); }
	private PortfolioEntity portfolio(UserEntity owner) { Instant now=Instant.now(); BrokerageEntity b=brokerages.saveAndFlush(new BrokerageEntity(UUID.randomUUID(),owner,"Broker","broker","61384004000105","Legal",null,"ACTIVE","BROKERS","01445000","Rua","Bairro","1",null,"São Paulo","SP",now,now)); return portfolios.saveAndFlush(new PortfolioEntity(UUID.randomUUID(),owner,b,"Portfolio "+UUID.randomUUID(),UUID.randomUUID().toString(),now,now)); }
	private record Session(String token, UserEntity user) { }

	@TestConfiguration(proxyBeanMethods=false) static class StubProviders {
		@Bean @Primary AssetSearchStrategyResolver assetSearchStrategyResolver() { return new AssetSearchStrategyResolver(List.of(new Stub(AssetMarket.BR),new Stub(AssetMarket.US))); }
		@Bean @Primary ExchangeRateStrategy exchangeRateStrategy() {
			return (source, target, date) -> EXCHANGE_AVAILABLE.get() && (HISTORICAL_EXCHANGE_AVAILABLE.get() || !date.isBefore(LocalDate.now()))
					? Optional.of(new ExchangeRate(source, target, new BigDecimal("5.00"), date.minusDays(1))) : Optional.empty();
		}
	}
	static class Stub implements AssetSearchStrategy { final AssetMarket market; Stub(AssetMarket market){this.market=market;} public AssetMarket market(){return market;} public List<AssetCandidate> findCandidates(AssetType type,String query){if("FAIL".equals(query)) throw new AssetProviderUnavailableException(market==AssetMarket.US?"ALPHAVANTAGE_RATE_LIMITED":"BRAPI_PROVIDER_UNAVAILABLE","provider unavailable"); if("EMPTY".equals(query)) return List.of(); return List.of(new AssetCandidate(market==AssetMarket.BR?"PETR4":"SPY",market==AssetMarket.BR?"Petrobras":"SPDR",market,type,market==AssetMarket.BR?"BRL":"USD"));} public Optional<AssetQuote> findQuote(String ticker){return Optional.of(new AssetQuote(ticker,market==AssetMarket.BR?"BRL":"USD",new BigDecimal("35.10")));} }
}
