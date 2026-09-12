package com.bominvestidor.spring.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategy;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;
import com.bominvestidor.spring.service.auth.AuthService;
import com.bominvestidor.spring.service.asset.RegisteredAssetService;
import com.bominvestidor.spring.dto.asset.RegisteredAssetCreateRequest;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.exception.RegisteredAssetConflictException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RegisteredAssetIntegrationTests.ProviderConfiguration.class)
class RegisteredAssetIntegrationTests {
	@Autowired MockMvc mockMvc;
	@Autowired AuthService authService;
	@Autowired RegisteredAssetService registeredAssetService;

	@Test
	void registersListsFiltersAndRejectsDuplicateAssets() throws Exception {
		Session owner = session();
		String selectionId = search(owner, "BR", "STOCK", "PETR");
		String created = mockMvc.perform(post("/api/assets").header("Authorization", owner.bearer())
				.contentType("application/json").content("{\"assetSelectionId\":\"" + selectionId + "\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.ticker").value("PETR4"))
				.andExpect(jsonPath("$.currency").value("BRL")).andExpect(jsonPath("$.lastQuote").isNumber())
				.andReturn().getResponse().getContentAsString();

		mockMvc.perform(get("/api/assets").header("Authorization", owner.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get("/api/assets").param("market", "US").header("Authorization", owner.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

		String duplicateSelection = search(owner, "BR", "STOCK", "PETR");
		mockMvc.perform(post("/api/assets").header("Authorization", owner.bearer())
				.contentType("application/json").content("{\"assetSelectionId\":\"" + duplicateSelection + "\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ASSET_ALREADY_REGISTERED"));

		String assetId = com.jayway.jsonpath.JsonPath.read(created, "$.id");
		mockMvc.perform(get("/api/assets/{id}/quote", assetId).header("Authorization", owner.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.assetId").value(assetId));
	}

	@Test
	void refreshesStoredQuoteButTransientQuoteDoesNotChangeCatalog() throws Exception {
		Session owner = session();
		String selectionId = search(owner, "US", "STOCK", "MSFT");
		String created = mockMvc.perform(post("/api/assets").header("Authorization", owner.bearer())
				.contentType("application/json").content("{\"assetSelectionId\":\"" + selectionId + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String assetId = com.jayway.jsonpath.JsonPath.read(created, "$.id");
		Number stored = com.jayway.jsonpath.JsonPath.read(created, "$.lastQuote");

		mockMvc.perform(get("/api/assets/{id}/quote", assetId).header("Authorization", owner.bearer()))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/assets").header("Authorization", owner.bearer()))
				.andExpect(jsonPath("$[0].lastQuote").value(stored.doubleValue()));
		mockMvc.perform(post("/api/assets/{id}/quote-refresh", assetId).header("Authorization", owner.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.lastQuote").isNumber());
	}

	@Test
	void deletesOwnedAssetWithoutPosition() throws Exception {
		Session owner = session();
		String selectionId = search(owner, "BR", "STOCK", "PETR");
		String created = mockMvc.perform(post("/api/assets").header("Authorization", owner.bearer())
				.contentType("application/json").content("{\"assetSelectionId\":\"" + selectionId + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String assetId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

		mockMvc.perform(delete("/api/assets/{id}", assetId).header("Authorization", owner.bearer()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/assets").header("Authorization", owner.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void exposesCatalogExchangeRateWithoutRequiringAPortfolio() throws Exception {
		Session owner = session();
		mockMvc.perform(get("/api/assets/exchange-rate").param("sourceCurrency", "BRL")
				.param("date", "2026-09-12").header("Authorization", owner.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.sourceCurrency").value("BRL"))
				.andExpect(jsonPath("$.targetCurrency").value("BRL"))
				.andExpect(jsonPath("$.rate").value(1));
	}

	@Test
	void isolatesCatalogAndProtectsAllRoutes() throws Exception {
		Session owner = session();
		String selectionId = search(owner, "BR", "ETF", "BOVA");
		String created = mockMvc.perform(post("/api/assets").header("Authorization", owner.bearer())
				.contentType("application/json").content("{\"assetSelectionId\":\"" + selectionId + "\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String assetId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

		Session other = session();
		mockMvc.perform(get("/api/assets").header("Authorization", other.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(get("/api/assets/{id}/quote", assetId).header("Authorization", other.bearer()))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REGISTERED_ASSET_NOT_FOUND"));
		mockMvc.perform(get("/api/assets")).andExpect(status().isUnauthorized());
		Jwt admin = Jwt.withTokenValue("admin").subject(UUID.randomUUID().toString())
				.claim("role", UserRole.ADMIN.name()).header("alg", "none").build();
		mockMvc.perform(get("/api/assets").with(jwt().jwt(admin))).andExpect(status().isForbidden());
	}

	@Test
	void translatesConcurrentDuplicateRegistrationAndPreservesQuoteOnProviderFailure() throws Exception {
		Session owner = session();
		String firstSelection = search(owner, "BR", "STOCK", "CONCURRENT");
		String secondSelection = search(owner, "BR", "STOCK", "CONCURRENT");
		var executor = Executors.newFixedThreadPool(2);
		var start = new CountDownLatch(1);
		try {
			var first = executor.submit(() -> registerAfter(start, owner.userId(), firstSelection));
			var second = executor.submit(() -> registerAfter(start, owner.userId(), secondSelection));
			start.countDown();
			List<String> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
			org.junit.jupiter.api.Assertions.assertEquals(1, results.stream().filter("CREATED"::equals).count());
			org.junit.jupiter.api.Assertions.assertEquals(1, results.stream().filter("ASSET_ALREADY_REGISTERED"::equals).count());
		} finally {
			executor.shutdownNow();
		}

		String assetId = com.jayway.jsonpath.JsonPath.read(
				mockMvc.perform(get("/api/assets").header("Authorization", owner.bearer())).andReturn()
						.getResponse().getContentAsString(), "$[0].id");
		Number stored = com.jayway.jsonpath.JsonPath.read(
				mockMvc.perform(get("/api/assets").header("Authorization", owner.bearer())).andReturn()
						.getResponse().getContentAsString(), "$[0].lastQuote");
		Stub.FAIL_QUOTES.set(true);
		try {
			mockMvc.perform(post("/api/assets/{id}/quote-refresh", assetId).header("Authorization", owner.bearer()))
					.andExpect(status().isServiceUnavailable());
			mockMvc.perform(get("/api/assets").header("Authorization", owner.bearer()))
					.andExpect(jsonPath("$[0].lastQuote").value(stored.doubleValue()));
		} finally {
			Stub.FAIL_QUOTES.set(false);
		}
	}

	private String registerAfter(CountDownLatch start, UUID ownerId, String selectionId) throws Exception {
		start.await(5, TimeUnit.SECONDS);
		try {
			registeredAssetService.register(ownerId, new RegisteredAssetCreateRequest(UUID.fromString(selectionId)));
			return "CREATED";
		} catch (RegisteredAssetConflictException exception) {
			return exception.getCode();
		}
	}

	private String search(Session session, String market, String type, String query) throws Exception {
		String result = mockMvc.perform(get("/api/assets/search").param("market", market).param("assetType", type)
				.param("query", query).header("Authorization", session.bearer()))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].selectionId").isNotEmpty())
				.andReturn().getResponse().getContentAsString();
		return com.jayway.jsonpath.JsonPath.read(result, "$[0].selectionId");
	}

	private Session session() {
		String email = UUID.randomUUID() + "@example.com";
		authService.register(new RegisterRequest("Investor", email, "password123"));
		var authentication = authService.login(new LoginRequest(email, "password123"));
		return new Session(authentication.token(), authentication.user().id());
	}

	private record Session(String token, UUID userId) {
		String bearer() { return "Bearer " + token; }
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class ProviderConfiguration {
		@Bean @Primary
		AssetSearchStrategyResolver registeredAssetStrategies() {
			return new AssetSearchStrategyResolver(List.of(new Stub(AssetMarket.BR), new Stub(AssetMarket.US)));
		}
	}

	static class Stub implements AssetSearchStrategy {
		private static final AtomicInteger QUOTES = new AtomicInteger(100);
		private static final AtomicBoolean FAIL_QUOTES = new AtomicBoolean(false);
		private final AssetMarket market;
		Stub(AssetMarket market) { this.market = market; }
		@Override public AssetMarket market() { return market; }
		@Override public List<AssetCandidate> findCandidates(AssetType type, String query) {
			String ticker = market == AssetMarket.BR ? (type == AssetType.ETF ? "BOVA11" : "PETR4") : "MSFT";
			return List.of(new AssetCandidate(ticker, "Ativo " + ticker, market, type,
					market == AssetMarket.BR ? "BRL" : "USD"));
		}
		@Override public Optional<AssetQuote> findQuote(String ticker) {
			if (FAIL_QUOTES.get()) throw new AssetProviderUnavailableException("ASSET_PROVIDER_UNAVAILABLE", "provider unavailable");
			return Optional.of(new AssetQuote(ticker, market == AssetMarket.BR ? "BRL" : "USD",
					new BigDecimal(QUOTES.incrementAndGet())));
		}
	}
}
