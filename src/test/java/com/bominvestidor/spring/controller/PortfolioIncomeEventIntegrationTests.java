package com.bominvestidor.spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.Jwt;

import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.income.IncomeEventCandidate;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventType;
import com.bominvestidor.spring.domain.income.IncomeProviderEvent;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;
import com.bominvestidor.spring.service.position.PortfolioPositionService;
import com.bominvestidor.spring.service.income.IncomeEventCandidateCache;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategy;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategyResolver;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategy;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.service.asset.AssetSelectionCache;
import com.bominvestidor.spring.domain.asset.SelectedAsset;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(PortfolioIncomeEventIntegrationTests.TestClockConfiguration.class)
class PortfolioIncomeEventIntegrationTests {
	private static final AtomicBoolean INCOME_PROVIDER_AVAILABLE = new AtomicBoolean(true);
	@Autowired MockMvc mockMvc; @Autowired AuthService auth; @Autowired UserRepository users; @Autowired BrokerageRepository brokerages;
	@Autowired PortfolioRepository portfolios; @Autowired PortfolioPositionService positions; @Autowired MutableClock clock;
	@Autowired IncomeEventCandidateCache candidateCache;
	@Autowired AssetSelectionCache selections;

	@Test
	void recordsManualIncomePreservesPositionsAndHandlesPendingLifecycle() throws Exception {
		clock.set(Instant.now());
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		buy(session, portfolioId, "10");
		LocalDate today = LocalDate.now(clock);
		String effective = mockMvc.perform(post(path(portfolioId)+"/manual").header("Authorization", bearer(session)).contentType("application/json")
				.content(manual(today.minusDays(2).toString(), "12.50"))).andExpect(status().isCreated()).andExpect(jsonPath("$.source").value("MANUAL"))
			.andExpect(jsonPath("$.status").value("EFFECTIVE")).andReturn().getResponse().getContentAsString();
		String pending = mockMvc.perform(post(path(portfolioId)+"/manual").header("Authorization", bearer(session)).contentType("application/json")
				.content(manual(today.plusDays(1).toString(), "10"))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
			.andReturn().getResponse().getContentAsString();
		String pendingId = com.jayway.jsonpath.JsonPath.read(pending, "$.id");
		mockMvc.perform(delete(path(portfolioId)+"/{id}", pendingId).header("Authorization", bearer(session))).andExpect(status().isNoContent());
		String effectiveId = com.jayway.jsonpath.JsonPath.read(effective, "$.id");
		mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(session))).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("CANCELLED"))
			.andExpect(jsonPath("$[1].id").value(effectiveId));
		mockMvc.perform(get("/api/portfolios/{id}/positions", portfolioId).header("Authorization", bearer(session))).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].quantity").value(10)).andExpect(jsonPath("$[0].averagePrice").value(35.10)).andExpect(jsonPath("$[0].custodyCost").value(351));
		mockMvc.perform(get("/api/portfolios/{id}/valuation", portfolioId).header("Authorization", bearer(session))).andExpect(status().isOk())
			.andExpect(jsonPath("$.positions[0].quantity").value(10)).andExpect(jsonPath("$.positions[0].averagePrice").value(35.10))
			.andExpect(jsonPath("$.positions[0].marketValue").value(400));
	}

	@Test
	void protectsRoutesAndValidatesManualIncome() throws Exception {
		clock.set(Instant.now()); Session session = session(); UUID portfolioId = portfolio(session.user()).getId(); buy(session, portfolioId, "1");
		String paymentDate = LocalDate.now(clock).minusDays(2).toString();
		mockMvc.perform(post(path(portfolioId)+"/manual").contentType("application/json").content(manual(paymentDate, "1"))).andExpect(status().isUnauthorized());
		mockMvc.perform(post(path(portfolioId)+"/manual").header("Authorization", bearer(session)).contentType("application/json").content(manual(paymentDate, "0")))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		Session other = session(); mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(other))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
		Jwt admin = Jwt.withTokenValue("admin").subject(UUID.randomUUID().toString()).claim("role", UserRole.ADMIN.name()).header("alg", "none").build();
		mockMvc.perform(get(path(portfolioId)).with(jwt().jwt(admin))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void queriesProviderCandidatesAndMapsUnavailableProviderWithoutInternalDetails() throws Exception {
		clock.set(Instant.now()); Session session = session(); UUID portfolioId = portfolio(session.user()).getId(); buy(session, portfolioId, "5");
		mockMvc.perform(get(path(portfolioId)+"/candidates").param("market", "BR").header("Authorization", bearer(session)))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].ticker").value("PETR4")).andExpect(jsonPath("$[0].eligibleQuantity").value(5))
			.andExpect(jsonPath("$[0].expectedAmount").value(5)).andExpect(jsonPath("$[0].confirmable").value(true));
		INCOME_PROVIDER_AVAILABLE.set(false);
		try {
			mockMvc.perform(get(path(portfolioId)+"/candidates").param("market", "BR").header("Authorization", bearer(session)))
				.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("BRAPI_PROVIDER_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("Provider unavailable")).andExpect(jsonPath("$.rawResponse").doesNotExist());
		} finally { INCOME_PROVIDER_AVAILABLE.set(true); }
	}

	@Test
	void confirmsCandidateRequiresReasonForAdjustmentAndPreventsDuplicates() throws Exception {
		clock.set(Instant.now()); Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		UUID candidateId = candidate(session, portfolioId);
		mockMvc.perform(post(path(portfolioId)+"/confirmations").header("Authorization", bearer(session)).contentType("application/json")
				.content("{\"candidateId\":\""+candidateId+"\",\"receivedAmount\":11}"))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("adjustmentReason"));
		mockMvc.perform(post(path(portfolioId)+"/confirmations").header("Authorization", bearer(session)).contentType("application/json")
				.content("{\"candidateId\":\""+candidateId+"\",\"receivedAmount\":11,\"adjustmentReason\":\"Tax withholding\"}"))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.expectedAmount").value(10)).andExpect(jsonPath("$.receivedAmount").value(11));
		UUID duplicateId = candidate(session, portfolioId);
		mockMvc.perform(post(path(portfolioId)+"/confirmations").header("Authorization", bearer(session)).contentType("application/json")
				.content("{\"candidateId\":\""+duplicateId+"\",\"receivedAmount\":10}"))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INCOME_EVENT_ALREADY_RECORDED"));
		String history = mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(session))).andReturn().getResponse().getContentAsString();
		String effectiveId = com.jayway.jsonpath.JsonPath.read(history, "$[0].id");
		mockMvc.perform(delete(path(portfolioId)+"/{id}", effectiveId).header("Authorization", bearer(session)))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INCOME_EVENT_CANNOT_BE_CANCELLED"));
	}

	@Test
	void rejectsManualIncomeForAssetNeverAcquired() throws Exception {
		clock.set(Instant.now()); Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		mockMvc.perform(post(path(portfolioId)+"/manual").header("Authorization", bearer(session)).contentType("application/json")
				.content(manual(LocalDate.now(clock).toString(), "1")))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ASSET_NOT_ACQUIRED"));
	}

	private UUID candidate(Session session, UUID portfolioId) {
		LocalDate today = LocalDate.now(clock);
		return candidateCache.store(session.user().getId(), portfolioId, new IncomeEventCandidate("brapi|PETR4|DIVIDEND|"+today.minusDays(1), "PETR4", "Petrobras", AssetMarket.BR,
				AssetType.STOCK, "BRL", IncomeEventType.DIVIDEND, IncomeEventSource.BRAPI, new java.math.BigDecimal("1"), today.minusDays(1), today,
				new java.math.BigDecimal("10"), new java.math.BigDecimal("10"), true, false));
	}

	private void buy(Session session, UUID portfolioId, String quantity) throws Exception { mockMvc.perform(post("/api/portfolios/{id}/transactions", portfolioId).header("Authorization", bearer(session)).contentType("application/json")
			.content("{\"assetSelectionId\":\""+selections.store(session.user().getId(), portfolioId, new SelectedAsset("PETR4", "Petrobras", AssetMarket.BR, AssetType.STOCK, "BRL"))+"\",\"type\":\"BUY\",\"transactionDate\":\""+LocalDate.now(clock).minusDays(10)+"\",\"quantity\":"+quantity+",\"unitPrice\":35.10}")).andExpect(status().isCreated()); }
	private String path(UUID portfolioId) { return "/api/portfolios/"+portfolioId+"/income-events"; }
	private String manual(String paymentDate, String amount) { return "{\"ticker\":\"PETR4\",\"type\":\"DIVIDEND\",\"paymentDate\":\""+paymentDate+"\",\"receivedAmount\":"+amount+"}"; }
	private String bearer(Session session) { return "Bearer "+session.token(); }
	private Session session() { String email=UUID.randomUUID()+"@example.com"; auth.register(new RegisterRequest("Investor",email,"password123")); return new Session(auth.login(new LoginRequest(email,"password123")).token(),users.findByEmail(email).orElseThrow()); }
	private PortfolioEntity portfolio(UserEntity owner) { Instant now=clock.instant(); BrokerageEntity brokerage=brokerages.saveAndFlush(new BrokerageEntity(UUID.randomUUID(),owner,"Broker","broker"+UUID.randomUUID(),"61384004000105","Legal",null,"ACTIVE","BROKERS","01445000","Rua","Bairro","1",null,"São Paulo","SP",now,now)); return portfolios.saveAndFlush(new PortfolioEntity(UUID.randomUUID(),owner,brokerage,"Portfolio "+UUID.randomUUID(),UUID.randomUUID().toString(),now,now)); }
	private record Session(String token, UserEntity user) { }
	@TestConfiguration(proxyBeanMethods=false) static class TestClockConfiguration {
		@Bean @Primary MutableClock clock() { return new MutableClock(Instant.parse("2026-06-01T12:00:00Z")); }
		@Bean @Primary IncomeEventProviderStrategyResolver incomeResolver() {
			return new IncomeEventProviderStrategyResolver(List.of(new IncomeEventProviderStrategy() {
				@Override public AssetMarket market() { return AssetMarket.BR; }
				@Override public List<IncomeProviderEvent> findEvents(String ticker) {
					if (!INCOME_PROVIDER_AVAILABLE.get()) throw new AssetProviderUnavailableException("BRAPI_PROVIDER_UNAVAILABLE", "Provider unavailable");
					LocalDate today = LocalDate.now();
					return List.of(new IncomeProviderEvent("stub|"+ticker, IncomeEventType.DIVIDEND, BigDecimal.ONE, today.minusDays(5), today, IncomeEventSource.BRAPI));
				}
			}));
		}
		@Bean @Primary AssetSearchStrategyResolver assetResolver() {
			return new AssetSearchStrategyResolver(List.of(new AssetSearchStrategy() {
				@Override public AssetMarket market() { return AssetMarket.BR; }
				@Override public List<AssetCandidate> findCandidates(AssetType type, String query) { return List.of(); }
				@Override public Optional<AssetQuote> findQuote(String ticker) { return Optional.of(new AssetQuote(ticker, "BRL", new BigDecimal("40"))); }
			}));
		}
	}
	static class MutableClock extends Clock { private Instant instant; MutableClock(Instant instant){this.instant=instant;} void set(Instant instant){this.instant=instant;} @Override public ZoneId getZone(){return ZoneOffset.UTC;} @Override public Clock withZone(ZoneId zone){return this;} @Override public Instant instant(){return instant;} }
}
