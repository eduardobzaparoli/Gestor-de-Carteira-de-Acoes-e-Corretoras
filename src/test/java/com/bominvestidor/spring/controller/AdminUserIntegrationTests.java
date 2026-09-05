package com.bominvestidor.spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserIntegrationTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private ObjectMapper objectMapper;
	@Autowired private UserRepository users;
	@Autowired private AuthService authService;
	@Autowired private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearUsers() {
		users.deleteAll();
	}

	@Test
	void administratorManagesAccountsWithoutExposingSensitiveOrFinancialData() throws Exception {
		String adminToken = administratorToken("admin@example.com");
		Map<String, Object> create = Map.of("name", " Investor ", "email", " INVESTOR@example.com ",
				"password", "password123", "role", "INVESTOR");

		MvcResult createResult = mockMvc.perform(post("/api/admin/users")
				.header("Authorization", bearer(adminToken)).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(create)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Investor"))
				.andExpect(jsonPath("$.email").value("investor@example.com"))
				.andExpect(jsonPath("$.role").value("INVESTOR"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.portfolios").doesNotExist())
				.andReturn();
		UUID investorId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

		mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == '" + investorId + "')]").exists());

		Map<String, Object> update = Map.of("name", "Updated Investor", "email", "updated@example.com",
				"password", "updated-password", "role", "INVESTOR");
		mockMvc.perform(put("/api/admin/users/{id}", investorId).header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(update)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Investor"))
				.andExpect(jsonPath("$.email").value("updated@example.com"));
	}

	@Test
	void deactivationBlocksLoginAndPreviouslyIssuedTokenUntilReactivation() throws Exception {
		String adminToken = administratorToken("admin@example.com");
		authService.register(new RegisterRequest("Investor", "investor@example.com", "password123"));
		UserEntity investor = users.findByEmail("investor@example.com").orElseThrow();
		String investorToken = authService.login(new LoginRequest("investor@example.com", "password123")).token();

		mockMvc.perform(delete("/api/admin/users/{id}", investor.getId()).header("Authorization", bearer(adminToken)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("email", "investor@example.com", "password", "password123"))))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));
		mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(investorToken)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("ACCOUNT_INACTIVE"));

		mockMvc.perform(post("/api/admin/users/{id}/reactivate", investor.getId()).header("Authorization", bearer(adminToken)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("email", "investor@example.com", "password", "password123"))))
				.andExpect(status().isOk());
	}

	@Test
	void protectsAdministrationFromInvestorsAndPreservesTheLastAdministrator() throws Exception {
		String adminToken = administratorToken("admin@example.com");
		authService.register(new RegisterRequest("Investor", "investor@example.com", "password123"));
		String investorToken = authService.login(new LoginRequest("investor@example.com", "password123")).token();
		UserEntity admin = users.findByEmail("admin@example.com").orElseThrow();

		mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(investorToken)))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(delete("/api/admin/users/{id}", admin.getId()).header("Authorization", bearer(adminToken)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CANNOT_DEACTIVATE_SELF"));

		Map<String, Object> demote = Map.of("name", "Admin", "email", "admin@example.com", "password", "password123", "role", "INVESTOR");
		mockMvc.perform(put("/api/admin/users/{id}", admin.getId()).header("Authorization", bearer(adminToken))
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(demote)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LAST_ACTIVE_ADMIN"));
	}

	private String administratorToken(String email) {
		Instant now = Instant.now();
		users.saveAndFlush(new UserEntity(UUID.randomUUID(), "Admin", email, passwordEncoder.encode("password123"),
				UserRole.ADMIN, UserStatus.ACTIVE, now, now));
		return authService.login(new LoginRequest(email, "password123")).token();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}
}
