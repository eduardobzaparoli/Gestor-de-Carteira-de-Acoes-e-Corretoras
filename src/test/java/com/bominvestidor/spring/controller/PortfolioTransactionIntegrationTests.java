package com.bominvestidor.spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.bominvestidor.spring.support.RegisteredAssetTestData.registeredAsset;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionCreateRequest;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.asset.RegisteredAssetRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.exception.PortfolioTransactionConflictException;
import com.bominvestidor.spring.service.auth.AuthService;
import com.bominvestidor.spring.service.transaction.PortfolioTransactionService;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(PortfolioTransactionIntegrationTests.TestClockConfiguration.class)
class PortfolioTransactionIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired AuthService auth; @Autowired UserRepository users; @Autowired BrokerageRepository brokerages; @Autowired PortfolioRepository portfolios; @Autowired PortfolioTransactionRepository transactions; @Autowired PortfolioTransactionService transactionService; @Autowired RegisteredAssetRepository registeredAssets; @Autowired MutableClock clock;

	@Test void recordsHistoryReservesFutureSalesAndPreservesPortfolioLog() throws Exception {
		clock.set(Instant.now());
		LocalDate today = LocalDate.now(clock); LocalDate pastDate = today.minusDays(2);
		LocalDate firstFutureDate = today.plusDays(1); LocalDate secondFutureDate = today.plusDays(2);
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body(session, portfolioId, "BUY", pastDate.toString(), "10")))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("EFFECTIVE"));
		String pending = mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body(session, portfolioId, "SELL", firstFutureDate.toString(), "7")))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andReturn().getResponse().getContentAsString();
		String pendingId = com.jayway.jsonpath.JsonPath.read(pending, "$.id");
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body(session, portfolioId, "SELL", secondFutureDate.toString(), "4")))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_ASSET_QUANTITY"));
		mockMvc.perform(delete(path(portfolioId)+"/{id}", pendingId).header("Authorization", bearer(session))).andExpect(status().isNoContent());
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body(session, portfolioId, "SELL", secondFutureDate.toString(), "10")))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
		mockMvc.perform(delete("/api/portfolios/{id}", portfolioId).header("Authorization", bearer(session)))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PORTFOLIO_HAS_TRANSACTIONS"));
		clock.set(secondFutureDate.plusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC));
		mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(session))).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].transactionDate").value(secondFutureDate.toString())).andExpect(jsonPath("$[0].status").value("EFFECTIVE"));
	}

	@Test void protectsRoutesAndRejectsInvalidRequests() throws Exception {
		clock.set(Instant.now());
		String pastDate = LocalDate.now(clock).minusDays(2).toString();
		Session first = session(); UUID portfolioId = portfolio(first.user()).getId();
		mockMvc.perform(post(path(portfolioId)).contentType("application/json").content(body(first, portfolioId, "BUY", pastDate, "1"))).andExpect(status().isUnauthorized());
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(first)).contentType("application/json").content(body(first, portfolioId, "BUY", pastDate, "0")))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
		Session other = session();
		mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(other))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
	}

	@Test void rejectsEffectiveSaleThatWouldConsumeFutureReservation() throws Exception {
		clock.set(Instant.now());
		LocalDate today = LocalDate.now(clock); Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json")
			.content(body(session, portfolioId, "BUY", today.minusDays(1).toString(), "10"))).andExpect(status().isCreated());
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json")
			.content(body(session, portfolioId, "SELL", today.plusDays(2).toString(), "7"))).andExpect(status().isCreated());
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json")
			.content(body(session, portfolioId, "SELL", today.toString(), "4"))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_ASSET_QUANTITY"));
	}

	@Test void keepsDuePendingSaleWhenItsEffectiveBalanceWouldBeNegative() throws Exception {
		clock.set(Instant.now());
		LocalDate today = LocalDate.now(clock); Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json")
			.content(body(session, portfolioId, "BUY", today.minusDays(1).toString(), "10"))).andExpect(status().isCreated());
		Instant now = clock.instant();
		PortfolioTransactionEntity invalidPendingSale = transactions.saveAndFlush(new PortfolioTransactionEntity(UUID.randomUUID(),
			portfolios.getReferenceById(portfolioId), "PETR4", "Petrobras", AssetMarket.BR, AssetType.STOCK, "BRL",
			TransactionType.SELL, TransactionStatus.PENDING, today.plusDays(1), new BigDecimal("11"), new BigDecimal("35.10"),
			BigDecimal.ZERO, now, now));
		clock.set(today.plusDays(2).atTime(12, 0).toInstant(ZoneOffset.UTC));
		mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(session))).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INSUFFICIENT_ASSET_QUANTITY"));
		assertEquals(TransactionStatus.PENDING, transactions.findById(invalidPendingSale.getId()).orElseThrow().getStatus());
	}

	@Test void allowsOnlyOneConcurrentSaleToConsumeTheSameQuantity() throws Exception {
		clock.set(Instant.now());
		LocalDate today = LocalDate.now(clock); Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json")
			.content(body(session, portfolioId, "BUY", today.minusDays(1).toString(), "10"))).andExpect(status().isCreated());
		CountDownLatch start = new CountDownLatch(1); ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			List<Future<String>> outcomes = new ArrayList<>();
			for (int index = 0; index < 2; index++) outcomes.add(executor.submit(() -> {
				start.await(5, TimeUnit.SECONDS);
				try { transactionService.create(session.user().getId(), portfolioId, new PortfolioTransactionCreateRequest(selection(session, portfolioId), TransactionType.SELL, today, new BigDecimal("7"), new BigDecimal("35.10"), BigDecimal.ZERO)); return "CREATED"; }
				catch (PortfolioTransactionConflictException exception) { return exception.getCode(); }
			}));
			start.countDown();
			List<String> results = new ArrayList<>();
			for (Future<String> outcome : outcomes) results.add(outcome.get(10, TimeUnit.SECONDS));
			assertEquals(1, results.stream().filter("CREATED"::equals).count());
			assertEquals(1, results.stream().filter("INSUFFICIENT_ASSET_QUANTITY"::equals).count());
		}
		finally { executor.shutdownNow(); }
		assertEquals(2, transactions.findAllByPortfolio_Id(portfolioId).size());
	}

	@Test void allowsCatalogAssetAcrossPortfoliosRejectsForeignAssetAndInvalidDecimalPrecision() throws Exception {
		clock.set(Instant.now()); Session owner = session(); UUID portfolioId = portfolio(owner.user()).getId();
		UUID assetId = selection(owner, portfolioId); PortfolioEntity original = portfolios.findById(portfolioId).orElseThrow(); Instant now = clock.instant();
		UUID otherPortfolioId = portfolios.saveAndFlush(new PortfolioEntity(UUID.randomUUID(), owner.user(), original.getBrokerage(), "Other portfolio", UUID.randomUUID().toString(), now, now)).getId();
		mockMvc.perform(post(path(otherPortfolioId)).header("Authorization", bearer(owner)).contentType("application/json")
				.content(request(assetId, "BUY", LocalDate.now(clock).toString(), "1", "35.10")))
			.andExpect(status().isCreated());
		Session other = session();
		UUID foreign = selection(other, portfolio(other.user()).getId());
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(owner)).contentType("application/json")
				.content(request(foreign, "BUY", LocalDate.now(clock).toString(), "1", "35.10")))
			.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REGISTERED_ASSET_NOT_FOUND"));
		UUID precise = selection(owner, portfolioId);
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(owner)).contentType("application/json")
				.content(request(precise, "BUY", LocalDate.now(clock).toString(), "1.123456789", "35.10")))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
	}

	@Test void editsOnlyPendingTransactionsAndRevalidatesTheirData() throws Exception {
		clock.set(Instant.now());
		LocalDate today = LocalDate.now(clock);
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		String pending = mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session))
				.contentType("application/json").content(body(session, portfolioId, "BUY", today.plusDays(2).toString(), "5")))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String pendingId = com.jayway.jsonpath.JsonPath.read(pending, "$.id");

		mockMvc.perform(put(path(portfolioId)+"/{id}", pendingId).header("Authorization", bearer(session))
				.contentType("application/json").content(updateBody("BUY", today.plusDays(3).toString(), "8", "42.30", "1.50")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.quantity").value(8)).andExpect(jsonPath("$.unitPrice").value(42.3))
				.andExpect(jsonPath("$.costs").value(1.5));

		String effective = mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session))
				.contentType("application/json").content(body(session, portfolioId, "BUY", today.toString(), "1")))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String effectiveId = com.jayway.jsonpath.JsonPath.read(effective, "$.id");
		mockMvc.perform(put(path(portfolioId)+"/{id}", effectiveId).header("Authorization", bearer(session))
				.contentType("application/json").content(updateBody("BUY", today.plusDays(2).toString(), "2", "40", "0")))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TRANSACTION_CANNOT_BE_EDITED"));
	}

	private String path(UUID portfolioId) { return "/api/portfolios/"+portfolioId+"/transactions"; }
	private UUID selection(Session session, UUID portfolioId) { return registeredAsset(registeredAssets, session.user(), "PETR4", "Petrobras", AssetMarket.BR, AssetType.STOCK, "BRL"); }
	private String bearer(Session session) { return "Bearer "+session.token(); }
	private String body(Session session, UUID portfolioId, String type, String date, String quantity) { return "{\"registeredAssetId\":\""+selection(session, portfolioId)+"\",\"type\":\""+type+"\",\"transactionDate\":\""+date+"\",\"quantity\":"+quantity+",\"unitPrice\":35.10}"; }
	private String request(UUID assetId, String type, String date, String quantity, String price) { return "{\"registeredAssetId\":\""+assetId+"\",\"type\":\""+type+"\",\"transactionDate\":\""+date+"\",\"quantity\":"+quantity+",\"unitPrice\":"+price+"}"; }
	private String updateBody(String type, String date, String quantity, String price, String costs) { return "{\"type\":\""+type+"\",\"transactionDate\":\""+date+"\",\"quantity\":"+quantity+",\"unitPrice\":"+price+",\"costs\":"+costs+"}"; }
	private Session session() { String email=UUID.randomUUID()+"@example.com"; auth.register(new RegisterRequest("Investor",email,"password123")); return new Session(auth.login(new LoginRequest(email,"password123")).token(),users.findByEmail(email).orElseThrow()); }
	private PortfolioEntity portfolio(UserEntity owner) { Instant now=clock.instant(); BrokerageEntity b=brokerages.saveAndFlush(new BrokerageEntity(UUID.randomUUID(),owner,"Broker","broker"+UUID.randomUUID(),"61384004000105","Legal",null,"ACTIVE","BROKERS","01445000","Rua","Bairro","1",null,"São Paulo","SP",now,now)); return portfolios.saveAndFlush(new PortfolioEntity(UUID.randomUUID(),owner,"unused".equals("x")?null:b,"Portfolio "+UUID.randomUUID(),UUID.randomUUID().toString(),now,now)); }
	private record Session(String token, UserEntity user) { }

	@TestConfiguration(proxyBeanMethods=false) static class TestClockConfiguration { @Bean @Primary MutableClock clock() { return new MutableClock(Instant.now()); } }
	static class MutableClock extends Clock { private Instant instant; MutableClock(Instant instant){this.instant=instant;} void set(Instant instant){this.instant=instant;} @Override public ZoneId getZone(){return ZoneOffset.UTC;} @Override public Clock withZone(ZoneId zone){return this;} @Override public Instant instant(){return instant;} }
}
