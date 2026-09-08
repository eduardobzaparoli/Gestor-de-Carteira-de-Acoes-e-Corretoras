package com.bominvestidor.spring.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.integration.address.AddressLookupData;
import com.bominvestidor.spring.integration.address.AddressLookupStrategy;
import com.bominvestidor.spring.integration.cnpj.CnpjLookupStrategy;
import com.bominvestidor.spring.integration.cnpj.CnpjRegistrationData;
import com.bominvestidor.spring.integration.cvm.CvmParticipantData;
import com.bominvestidor.spring.integration.cvm.CvmParticipantStrategy;
import com.bominvestidor.spring.service.auth.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BrokerageIntegrationTests.StubProviders.class)
class BrokerageIntegrationTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private AuthService authService;

	@Test
	void createsAndListsBrokerageUsingOfficialCityAndState() throws Exception {
		String token = registerAndLogin();
		String body = """
				{"nickname":"Minha Corretora","cnpj":"04.252.011/0001-10","cep":"04547-000",
				"street":"Rua manual","neighborhood":"Bairro manual","number":"42","complement":"Sala 1",
				"city":"Cidade controlada","state":"XX"}
				""";

		mockMvc.perform(post("/api/brokerages").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nickname").value("Minha Corretora"))
				.andExpect(jsonPath("$.cnpj").value("04252011000110"))
				.andExpect(jsonPath("$.address.street").value("Rua manual"))
				.andExpect(jsonPath("$.address.city").value("São Paulo"))
				.andExpect(jsonPath("$.address.state").value("SP"))
				.andExpect(jsonPath("$.ownerId").doesNotExist());

		mockMvc.perform(get("/api/brokerages").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nickname").value("Minha Corretora"));
	}

	@Test
	void protectsBrokerageOperationsAndValidatesMalformedRegistration() throws Exception {
		mockMvc.perform(get("/api/brokerages")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		Jwt admin = Jwt.withTokenValue("admin-token").subject(UUID.randomUUID().toString())
				.claim("role", UserRole.ADMIN.name()).header("alg", "none").build();
		mockMvc.perform(get("/api/brokerages").with(jwt().jwt(admin)))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));

		String token = registerAndLogin();
		mockMvc.perform(post("/api/brokerages").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void providesProtectedCepLookupAndRejectsInvalidCep() throws Exception {
		String token = registerAndLogin();

		mockMvc.perform(get("/api/brokerages/cep/04547-000").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cep").value("04547-000"))
				.andExpect(jsonPath("$.city").value("São Paulo"))
				.andExpect(jsonPath("$.state").value("SP"));

		mockMvc.perform(get("/api/brokerages/cep/123").header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'cep')]").exists());
	}

	@Test
	void providesCompanyLookupAndDeletesUnlinkedBrokerage() throws Exception {
		String token = registerAndLogin();
		mockMvc.perform(get("/api/brokerages/cnpj").param("cnpj", "04.252.011/0001-10")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.legalName").value("Razão Social"));

		String body = """
				{"nickname":"Temporária","cnpj":"04.252.011/0001-10","cep":"04547-000",
				"street":"Rua manual","neighborhood":"Bairro manual","number":"42"}
				""";
		String created = mockMvc.perform(post("/api/brokerages").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

		mockMvc.perform(delete("/api/brokerages/{id}", id).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/brokerages").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
	}

	private String registerAndLogin() {
		String email = "brokerage-" + UUID.randomUUID() + "@example.com";
		authService.register(new com.bominvestidor.spring.dto.auth.RegisterRequest("Investidor", email, "password123"));
		return authService.login(new com.bominvestidor.spring.dto.auth.LoginRequest(email, "password123")).token();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class StubProviders {
		@Bean @Primary
		CnpjLookupStrategy cnpjLookupStrategy() {
			return cnpj -> java.util.Optional.of(new CnpjRegistrationData(cnpj, "Razão Social", "Nome Fantasia", "04547000"));
		}

		@Bean @Primary
		AddressLookupStrategy addressLookupStrategy() {
			return cep -> java.util.Optional.of(new AddressLookupData("04547-000", "Rua oficial", "Bairro oficial", "São Paulo", "SP"));
		}

		@Bean @Primary
		CvmParticipantStrategy cvmParticipantStrategy() {
			return cnpj -> java.util.Optional.of(new CvmParticipantData(cnpj, "CORRETORAS", "EM FUNCIONAMENTO NORMAL"));
		}
	}
}
