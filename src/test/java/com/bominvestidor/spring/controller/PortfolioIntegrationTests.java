package com.bominvestidor.spring.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioIntegrationTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private AuthService authService;
	@Autowired private UserRepository userRepository;
	@Autowired private BrokerageRepository brokerageRepository;

	@Test
	void createsNormalizesListsAndDeletesPortfolio() throws Exception {
		Session investor = registerAndLogin();
		BrokerageEntity brokerage = saveBrokerage(investor.user());
		String body = """
				{"name":"  Carteira de longo prazo  ","brokerageId":"%s","ownerId":"%s",
				"brokerage":{"nickname":"não deve controlar"}}
				""".formatted(brokerage.getId(), UUID.randomUUID());

		MvcResult create = mockMvc.perform(post("/api/portfolios").header("Authorization", "Bearer " + investor.token())
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Carteira de longo prazo"))
				.andExpect(jsonPath("$.brokerage.id").value(brokerage.getId().toString()))
				.andExpect(jsonPath("$.ownerId").doesNotExist())
				.andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(create.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/portfolios").header("Authorization", "Bearer " + investor.token()))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
		mockMvc.perform(get("/api/portfolios/{id}", id).header("Authorization", "Bearer " + investor.token()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Carteira de longo prazo"));
		mockMvc.perform(post("/api/portfolios").header("Authorization", "Bearer " + investor.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"CARTEIRA DE LONGO PRAZO\",\"brokerageId\":\"" + brokerage.getId() + "\"}"))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PORTFOLIO_NAME_ALREADY_REGISTERED"));
		mockMvc.perform(post("/api/portfolios").header("Authorization", "Bearer " + investor.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Carteira para renda\",\"brokerageId\":\"" + brokerage.getId() + "\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/portfolios").header("Authorization", "Bearer " + investor.token()))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Carteira de longo prazo"))
				.andExpect(jsonPath("$[1].name").value("Carteira para renda"));

		BrokerageEntity newBrokerage = saveBrokerage(investor.user());
		mockMvc.perform(put("/api/portfolios/{id}", id).header("Authorization", "Bearer " + investor.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"  Carteira revisada  \",\"brokerageId\":\"" + newBrokerage.getId() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.name").value("Carteira revisada"))
				.andExpect(jsonPath("$.brokerage.id").value(newBrokerage.getId().toString()));
		mockMvc.perform(put("/api/portfolios/{id}", id).header("Authorization", "Bearer " + investor.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"CARTEIRA PARA RENDA\",\"brokerageId\":\"" + newBrokerage.getId() + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PORTFOLIO_NAME_ALREADY_REGISTERED"));
		mockMvc.perform(get("/api/portfolios/{id}", id).header("Authorization", "Bearer " + investor.token()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Carteira revisada"));

		mockMvc.perform(delete("/api/portfolios/{id}", id).header("Authorization", "Bearer " + investor.token()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/portfolios/{id}", id).header("Authorization", "Bearer " + investor.token()))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
	}

	@Test
	void validatesBrokerageOwnershipAndPreventsPortfolioDisclosure() throws Exception {
		Session first = registerAndLogin();
		Session second = registerAndLogin();
		BrokerageEntity firstBrokerage = saveBrokerage(first.user());
		BrokerageEntity secondBrokerage = saveBrokerage(second.user());

		mockMvc.perform(get("/api/portfolios").header("Authorization", "Bearer " + first.token()))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(post("/api/portfolios").header("Authorization", "Bearer " + first.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Carteira alheia\",\"brokerageId\":\"" + secondBrokerage.getId() + "\"}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BROKERAGE_NOT_FOUND"));
		MvcResult create = mockMvc.perform(post("/api/portfolios").header("Authorization", "Bearer " + first.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Carteira privada\",\"brokerageId\":\"" + firstBrokerage.getId() + "\"}"))
				.andExpect(status().isCreated()).andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(create.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/portfolios/{id}", id).header("Authorization", "Bearer " + second.token()))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
		mockMvc.perform(put("/api/portfolios/{id}", id).header("Authorization", "Bearer " + second.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Tentativa\",\"brokerageId\":\"" + secondBrokerage.getId() + "\"}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"));
		mockMvc.perform(put("/api/portfolios/{id}", id).header("Authorization", "Bearer " + first.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Tentativa\",\"brokerageId\":\"" + secondBrokerage.getId() + "\"}"))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BROKERAGE_NOT_FOUND"));
	}

	@Test
	void protectsAllOperationsAndValidatesInput() throws Exception {
		mockMvc.perform(get("/api/portfolios")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		Jwt admin = Jwt.withTokenValue("admin-token").subject(UUID.randomUUID().toString())
				.claim("role", UserRole.ADMIN.name()).header("alg", "none").build();
		mockMvc.perform(get("/api/portfolios").with(jwt().jwt(admin))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(put("/api/portfolios/{id}", UUID.randomUUID()).with(jwt().jwt(admin))
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Carteira\",\"brokerageId\":\""
						+ UUID.randomUUID() + "\"}"))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));

		Session investor = registerAndLogin();
		mockMvc.perform(post("/api/portfolios").header("Authorization", "Bearer " + investor.token())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
		mockMvc.perform(post("/api/portfolios").header("Authorization", "Bearer " + investor.token())
				.contentType(MediaType.APPLICATION_JSON).content("{malformed"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
	}

	private Session registerAndLogin() {
		String email = "portfolio-" + UUID.randomUUID() + "@example.com";
		authService.register(new RegisterRequest("Investidor", email, "password123"));
		String token = authService.login(new LoginRequest(email, "password123")).token();
		return new Session(token, userRepository.findByEmail(email).orElseThrow());
	}

	private BrokerageEntity saveBrokerage(UserEntity owner) {
		UUID id = UUID.randomUUID();
		Instant now = Instant.now();
		String cnpj = id.toString().replace("-", "").substring(0, 14);
		return brokerageRepository.saveAndFlush(new BrokerageEntity(id, owner, "Corretora " + id, "corretora " + id,
				cnpj, "Razão Social", "Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS", "04547000",
				"Rua", "Bairro", "1", null, "São Paulo", "SP", now, now));
	}

	private record Session(String token, UserEntity user) {
	}
}
