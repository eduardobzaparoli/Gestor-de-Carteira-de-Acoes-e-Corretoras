package com.bominvestidor.spring.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.bominvestidor.spring.support.RegisteredAssetTestData.registeredAsset;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

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

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.asset.RegisteredAssetRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PortfolioPositionIntegrationTests.TestClockConfiguration.class)
class PortfolioPositionIntegrationTests {
	@Autowired MockMvc mockMvc;
	@Autowired AuthService auth;
	@Autowired UserRepository users;
	@Autowired BrokerageRepository brokerages;
	@Autowired PortfolioRepository portfolios;
	@Autowired MutableClock clock;
	@Autowired RegisteredAssetRepository registeredAssets;

	@Test
	void calculatesWeightedAveragePartialSalesFullSettlementAndReopening() throws Exception {
		clock.set(Instant.now());
		String today = LocalDate.now(clock).toString(); String yesterday = LocalDate.now(clock).minusDays(1).toString();
		String twoDaysAgo = LocalDate.now(clock).minusDays(2).toString();
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		post(session, portfolioId, transaction("PETR4", "Petrobras antigo", "BR", "STOCK", "BRL", "BUY", twoDaysAgo, "10", "10", "2"))
			.andExpect(status().isCreated());
		post(session, portfolioId, transaction("PETR4", "Petrobras atual", "BR", "STOCK", "BRL", "BUY", yesterday, "5", "20", "1"))
			.andExpect(status().isCreated());
		post(session, portfolioId, transaction("PETR4", "Petrobras venda", "BR", "STOCK", "BRL", "SELL", today, "4", "99", "7"))
			.andExpect(status().isCreated());

		mockMvc.perform(get(positionsPath(portfolioId)).header("Authorization", bearer(session)))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].ticker").value("PETR4"))
			.andExpect(jsonPath("$[0].assetName").value("Petrobras antigo"))
			.andExpect(jsonPath("$[0].quantity").value(11))
			.andExpect(jsonPath("$[0].averagePrice").value(13.53333333));

		post(session, portfolioId, transaction("PETR4", "Petrobras venda", "BR", "STOCK", "BRL", "SELL", today, "11", "1", "0"))
			.andExpect(status().isCreated());
		mockMvc.perform(get(positionsPath(portfolioId)).header("Authorization", bearer(session)))
			.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		post(session, portfolioId, transaction("PETR4", "Petrobras reaberta", "BR", "STOCK", "BRL", "BUY", today, "2", "7", "0"))
			.andExpect(status().isCreated());
		mockMvc.perform(get(positionsPath(portfolioId)).header("Authorization", bearer(session)))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].quantity").value(2))
			.andExpect(jsonPath("$[0].averagePrice").value(7));
	}

	@Test
	void excludesPendingAndCancelledTransactionsOrdersGroupsAndReconcilesDuePending() throws Exception {
		clock.set(Instant.now());
		String yesterday = LocalDate.now(clock).minusDays(1).toString(); String tomorrow = LocalDate.now(clock).plusDays(1).toString();
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		post(session, portfolioId, transaction("MSFT", "Microsoft", "US", "STOCK", "USD", "BUY", yesterday, "1", "100", "0"));
		post(session, portfolioId, transaction("BOVA11", "BOVA11", "BR", "ETF", "BRL", "BUY", yesterday, "1", "170", "0"));
		post(session, portfolioId, transaction("PETR4", "Pendente", "BR", "STOCK", "BRL", "BUY", tomorrow, "1", "10", "0"));
		String cancelled = post(session, portfolioId, transaction("VALE3", "Cancelada", "BR", "STOCK", "BRL", "BUY", tomorrow, "1", "10", "0"))
			.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String cancelledId = com.jayway.jsonpath.JsonPath.read(cancelled, "$.id");
		mockMvc.perform(delete("/api/portfolios/{portfolioId}/transactions/{transactionId}", portfolioId, cancelledId)
			.header("Authorization", bearer(session))).andExpect(status().isNoContent());
		mockMvc.perform(get(positionsPath(portfolioId)).header("Authorization", bearer(session)))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].ticker").value("BOVA11"))
			.andExpect(jsonPath("$[0].id").doesNotExist()).andExpect(jsonPath("$[1].ticker").value("MSFT")).andExpect(jsonPath("$.length()").value(2));
		clock.set(clock.instant().plusSeconds(2 * 24 * 60 * 60));
		mockMvc.perform(get(positionsPath(portfolioId)).header("Authorization", bearer(session)))
			.andExpect(status().isOk()).andExpect(jsonPath("$[1].ticker").value("PETR4"))
			.andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void rejectsRetroactiveSaleThatWouldCreateNegativeHistoricalBalance() throws Exception {
		clock.set(Instant.now());
		String today = LocalDate.now(clock).toString(); String yesterday = LocalDate.now(clock).minusDays(1).toString();
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		post(session, portfolioId, transaction("PETR4", "Petrobras", "BR", "STOCK", "BRL", "BUY", today, "10", "10", "0"));
		post(session, portfolioId, transaction("PETR4", "Petrobras", "BR", "STOCK", "BRL", "SELL", yesterday, "1", "10", "0"))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_ASSET_QUANTITY"));
		clock.advance(Duration.ofSeconds(1));
		post(session, portfolioId, transaction("PETR4", "Petrobras", "BR", "STOCK", "BRL", "BUY", yesterday, "2", "10", "0"))
			.andExpect(status().isCreated());
		clock.advance(Duration.ofSeconds(1));
		post(session, portfolioId, transaction("PETR4", "Petrobras", "BR", "STOCK", "BRL", "SELL", yesterday, "1", "10", "0"))
			.andExpect(status().isCreated());
	}

	@Test
	void protectsPrivatePositionsAndDoesNotDiscloseOtherInvestorPortfolio() throws Exception {
		clock.set(Instant.now());
		Session first = session(); UUID portfolioId = portfolio(first.user()).getId();
		mockMvc.perform(get(positionsPath(portfolioId))).andExpect(status().isUnauthorized());
		Session other = session();
		mockMvc.perform(get(positionsPath(portfolioId)).header("Authorization", bearer(other)))
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
		Jwt admin = Jwt.withTokenValue("admin-token").subject(UUID.randomUUID().toString()).claim("role", UserRole.ADMIN.name()).header("alg", "none").build();
		mockMvc.perform(get(positionsPath(portfolioId)).with(jwt().jwt(admin))).andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.ResultActions post(Session session, UUID portfolioId, String body) throws Exception {
		String ticker = com.jayway.jsonpath.JsonPath.read(body, "$.ticker");
		String name = com.jayway.jsonpath.JsonPath.read(body, "$.assetName");
		String market = com.jayway.jsonpath.JsonPath.read(body, "$.market");
		String type = com.jayway.jsonpath.JsonPath.read(body, "$.assetType");
		String currency = com.jayway.jsonpath.JsonPath.read(body, "$.currency");
		UUID assetId = registeredAsset(registeredAssets, session.user(), ticker, name, AssetMarket.valueOf(market), AssetType.valueOf(type), currency);
		String request = "{\"registeredAssetId\":\"%s\",\"type\":\"%s\",\"transactionDate\":\"%s\",\"quantity\":%s,\"unitPrice\":%s,\"costs\":%s}"
				.formatted(assetId, com.jayway.jsonpath.JsonPath.read(body, "$.type"), com.jayway.jsonpath.JsonPath.read(body, "$.transactionDate"), com.jayway.jsonpath.JsonPath.read(body, "$.quantity"), com.jayway.jsonpath.JsonPath.read(body, "$.unitPrice"), com.jayway.jsonpath.JsonPath.read(body, "$.costs"));
		return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/portfolios/{portfolioId}/transactions", portfolioId).header("Authorization", bearer(session)).contentType("application/json").content(request));
	}
	private String transaction(String ticker, String name, String market, String type, String currency, String transactionType,
			String date, String quantity, String price, String costs) {
		return "{\"ticker\":\"%s\",\"assetName\":\"%s\",\"market\":\"%s\",\"assetType\":\"%s\",\"currency\":\"%s\",\"type\":\"%s\",\"transactionDate\":\"%s\",\"quantity\":%s,\"unitPrice\":%s,\"costs\":%s}"
			.formatted(ticker, name, market, type, currency, transactionType, date, quantity, price, costs);
	}
	private String positionsPath(UUID portfolioId) { return "/api/portfolios/" + portfolioId + "/positions"; }
	private String bearer(Session session) { return "Bearer " + session.token(); }
	private Session session() { String email = UUID.randomUUID()+"@example.com"; auth.register(new RegisterRequest("Investor", email, "password123")); return new Session(auth.login(new LoginRequest(email, "password123")).token(), users.findByEmail(email).orElseThrow()); }
	private PortfolioEntity portfolio(UserEntity owner) { Instant now = clock.instant(); BrokerageEntity b = brokerages.saveAndFlush(new BrokerageEntity(UUID.randomUUID(), owner, "Broker", "broker"+UUID.randomUUID(), "61384004000105", "Legal", null, "ACTIVE", "BROKERS", "01445000", "Rua", "Bairro", "1", null, "São Paulo", "SP", now, now)); return portfolios.saveAndFlush(new PortfolioEntity(UUID.randomUUID(), owner, b, "Portfolio "+UUID.randomUUID(), UUID.randomUUID().toString(), now, now)); }
	private record Session(String token, UserEntity user) { }

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClockConfiguration { @Bean @Primary MutableClock clock() { return new MutableClock(Instant.now()); } }
	static class MutableClock extends Clock { private Instant instant; MutableClock(Instant instant) { this.instant=instant; } void set(Instant instant) { this.instant=instant; } void advance(Duration duration) { instant = instant.plus(duration); } @Override public ZoneId getZone(){return ZoneOffset.UTC;} @Override public Clock withZone(ZoneId zone){return this;} @Override public Instant instant(){return instant;} }
}
