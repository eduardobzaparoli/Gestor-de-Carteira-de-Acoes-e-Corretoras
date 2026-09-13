package com.bominvestidor.spring.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.income.PortfolioIncomeEventRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;
import com.bominvestidor.spring.support.IntegrationTestDataCleaner;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InvestorProfileIntegrationTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private AuthService authService;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private PortfolioIncomeEventRepository incomeEvents;
	@Autowired private PortfolioTransactionRepository transactions;
	@Autowired private PortfolioRepository portfolios;
	@Autowired private BrokerageRepository brokerages;
	@Autowired private UserRepository users;

	@BeforeEach
	void clearData() {
		new IntegrationTestDataCleaner(incomeEvents, transactions, portfolios, brokerages, users).clear();
	}

	@Test
	void updatesOwnNormalizedProfileAndPasswordWithoutExposingSensitiveFields() throws Exception {
		String token = registerAndLogin("Ana", "ana@example.com", "current-password");
		String oldHash = users.findByEmail("ana@example.com").orElseThrow().getPasswordHash();

		mockMvc.perform(put("/api/auth/me").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"name", "  Ana Atualizada  ",
						"email", " ANA.NOVA@EXAMPLE.COM ",
						"currentPassword", "current-password",
						"newPassword", "new-password"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ana Atualizada"))
				.andExpect(jsonPath("$.email").value("ana.nova@example.com"))
				.andExpect(jsonPath("$.role").value("INVESTOR"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		var saved = users.findByEmail("ana.nova@example.com").orElseThrow();
		assertNotEquals(oldHash, saved.getPasswordHash());
		assertTrue(passwordEncoder.matches("new-password", saved.getPasswordHash()));
		assertEquals("Ana Atualizada", saved.getName());
	}

	@Test
	void keepsOwnEmailAndRejectsEmailFromAnotherAccountAtomically() throws Exception {
		String anaToken = registerAndLogin("Ana", "ana@example.com", "password123");
		registerAndLogin("Bia", "bia@example.com", "password456");

		mockMvc.perform(put("/api/auth/me").header("Authorization", bearer(anaToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("name", "Ana Mesmo Email",
						"email", " ANA@EXAMPLE.COM "))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ana@example.com"));

		mockMvc.perform(put("/api/auth/me").header("Authorization", bearer(anaToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("name", "Nome Indevido",
						"email", "bia@example.com"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));

		var ana = users.findByEmail("ana@example.com").orElseThrow();
		assertEquals("Ana Mesmo Email", ana.getName());
	}

	@Test
	void rejectsInvalidPasswordAndRequestWithoutPartialUpdate() throws Exception {
		String token = registerAndLogin("Ana", "ana@example.com", "current-password");

		mockMvc.perform(put("/api/auth/me").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("name", "Nome Não Salvo",
						"email", "changed@example.com", "currentPassword", "wrong-password",
						"newPassword", "new-password"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));

		var saved = users.findByEmail("ana@example.com").orElseThrow();
		assertEquals("Ana", saved.getName());
		assertTrue(passwordEncoder.matches("current-password", saved.getPasswordHash()));
	}

	@Test
	void forbidsProfileUpdateWithoutInvestorAuthentication() throws Exception {
		Map<String, Object> body = Map.of("name", "Ana", "email", "ana@example.com");
		mockMvc.perform(put("/api/auth/me").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void forbidsProfileUpdateForAdministrator() throws Exception {
		Jwt administrator = Jwt.withTokenValue("admin-token").subject(UUID.randomUUID().toString())
				.claim("role", UserRole.ADMIN.name()).header("alg", "none").build();

		mockMvc.perform(put("/api/auth/me").with(jwt().jwt(administrator))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("name", "Admin", "email", "admin@example.com"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void resolvesConcurrentEmailUpdatesWithOneConflict() throws Exception {
		String anaToken = registerAndLogin("Ana", "ana@example.com", "password123");
		String biaToken = registerAndLogin("Bia", "bia@example.com", "password456");
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<Integer> ana = executor.submit(() -> updateAfter(start, anaToken, "Ana", "shared@example.com"));
			Future<Integer> bia = executor.submit(() -> updateAfter(start, biaToken, "Bia", "shared@example.com"));
			start.countDown();
			List<Integer> statuses = List.of(ana.get(), bia.get()).stream().sorted().toList();
			assertEquals(List.of(200, 409), statuses);
		}
		finally {
			executor.shutdownNow();
		}
		assertEquals(1, users.findAll().stream().filter(user -> "shared@example.com".equals(user.getEmail())).count());
	}

	private int updateAfter(CountDownLatch start, String token, String name, String email) throws Exception {
		start.await();
		return mockMvc.perform(put("/api/auth/me").header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("name", name, "email", email))))
				.andReturn().getResponse().getStatus();
	}

	private String registerAndLogin(String name, String email, String password) {
		authService.register(new RegisterRequest(name, email, password));
		return authService.login(new LoginRequest(email, password)).token();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
