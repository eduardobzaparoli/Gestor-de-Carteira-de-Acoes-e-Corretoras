package com.bominvestidor.spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.Instant;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(PortfolioTransactionIntegrationTests.TestClockConfiguration.class)
class PortfolioTransactionIntegrationTests {
	@Autowired MockMvc mockMvc; @Autowired AuthService auth; @Autowired UserRepository users; @Autowired BrokerageRepository brokerages; @Autowired PortfolioRepository portfolios; @Autowired MutableClock clock;

	@Test void recordsHistoryReservesFutureSalesAndPreservesPortfolioLog() throws Exception {
		Session session = session(); UUID portfolioId = portfolio(session.user()).getId();
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body("BUY", "2026-08-28", "10")))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("EFFECTIVE"));
		String pending = mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body("SELL", "2026-08-31", "7")))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andReturn().getResponse().getContentAsString();
		String pendingId = com.jayway.jsonpath.JsonPath.read(pending, "$.id");
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body("SELL", "2026-09-01", "4")))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INSUFFICIENT_ASSET_QUANTITY"));
		mockMvc.perform(delete(path(portfolioId)+"/{id}", pendingId).header("Authorization", bearer(session))).andExpect(status().isNoContent());
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(session)).contentType("application/json").content(body("SELL", "2026-09-01", "10")))
			.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
		mockMvc.perform(delete("/api/portfolios/{id}", portfolioId).header("Authorization", bearer(session)))
			.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PORTFOLIO_HAS_TRANSACTIONS"));
		clock.set(Instant.parse("2026-09-02T12:00:00Z"));
		mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(session))).andExpect(status().isOk())
			.andExpect(jsonPath("$[0].transactionDate").value("2026-09-01")).andExpect(jsonPath("$[0].status").value("EFFECTIVE"));
	}

	@Test void protectsRoutesAndRejectsInvalidRequests() throws Exception {
		Session first = session(); UUID portfolioId = portfolio(first.user()).getId();
		mockMvc.perform(post(path(portfolioId)).contentType("application/json").content(body("BUY", "2026-08-28", "1"))).andExpect(status().isUnauthorized());
		mockMvc.perform(post(path(portfolioId)).header("Authorization", bearer(first)).contentType("application/json").content(body("BUY", "2026-08-28", "0")))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
		Session other = session();
		mockMvc.perform(get(path(portfolioId)).header("Authorization", bearer(other))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
	}

	private String path(UUID portfolioId) { return "/api/portfolios/"+portfolioId+"/transactions"; }
	private String bearer(Session session) { return "Bearer "+session.token(); }
	private String body(String type, String date, String quantity) { return "{\"ticker\":\"PETR4\",\"assetName\":\"Petrobras\",\"market\":\"BR\",\"assetType\":\"STOCK\",\"currency\":\"BRL\",\"type\":\""+type+"\",\"transactionDate\":\""+date+"\",\"quantity\":"+quantity+",\"unitPrice\":35.10}"; }
	private Session session() { String email=UUID.randomUUID()+"@example.com"; auth.register(new RegisterRequest("Investor",email,"password123")); return new Session(auth.login(new LoginRequest(email,"password123")).token(),users.findByEmail(email).orElseThrow()); }
	private PortfolioEntity portfolio(UserEntity owner) { Instant now=clock.instant(); BrokerageEntity b=brokerages.saveAndFlush(new BrokerageEntity(UUID.randomUUID(),owner,"Broker","broker"+UUID.randomUUID(),"61384004000105","Legal",null,"ACTIVE","BROKERS","01445000","Rua","Bairro","1",null,"São Paulo","SP",now,now)); return portfolios.saveAndFlush(new PortfolioEntity(UUID.randomUUID(),owner,"unused".equals("x")?null:b,"Portfolio "+UUID.randomUUID(),UUID.randomUUID().toString(),now,now)); }
	private record Session(String token, UserEntity user) { }

	@TestConfiguration(proxyBeanMethods=false) static class TestClockConfiguration { @Bean @Primary MutableClock clock() { return new MutableClock(Instant.now()); } }
	static class MutableClock extends Clock { private Instant instant; MutableClock(Instant instant){this.instant=instant;} void set(Instant instant){this.instant=instant;} @Override public ZoneId getZone(){return ZoneOffset.UTC;} @Override public Clock withZone(ZoneId zone){return this;} @Override public Instant instant(){return instant;} }
}
