package com.bominvestidor.spring.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.income.PortfolioIncomeEventRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.support.IntegrationTestDataCleaner;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BrokerageRepository brokerageRepository;

	@Autowired
	private PortfolioRepository portfolioRepository;

	@Autowired
	private PortfolioTransactionRepository transactionRepository;

	@Autowired
	private PortfolioIncomeEventRepository incomeEventRepository;

	@Autowired
	private JwtEncoder jwtEncoder;

	@BeforeEach
	void clearUsers() {
		new IntegrationTestDataCleaner(incomeEventRepository, transactionRepository, portfolioRepository, brokerageRepository,
				userRepository).clear();
	}

	@Test
	void registersNormalizedInvestorWithoutExposingSensitiveFields() throws Exception {
		Map<String, Object> body = Map.of(
				"name", "  Investidor  ",
				"email", " INVESTIDOR@EXAMPLE.COM ",
				"password", "password123",
				"role", "ADMIN");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Investidor"))
				.andExpect(jsonPath("$.email").value("investidor@example.com"))
				.andExpect(jsonPath("$.role").value("INVESTOR"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void returnsFieldErrorsForInvalidRegistration() throws Exception {
		Map<String, Object> body = Map.of(
				"name", " " + "a".repeat(101) + " ",
				"email", "invalid-email",
				"password", "short");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
				.andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
	}

	@Test
	void rejectsDuplicateEmailIgnoringCaseAndWhitespace() throws Exception {
		register("Investidor", "investidor@example.com", "password123");

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"name", "Outro",
						"email", " INVESTIDOR@EXAMPLE.COM ",
						"password", "password456"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void logsInAndReturnsAuthenticatedIdentity() throws Exception {
		register("Investidor", "investidor@example.com", "password123");
		String token = loginAndGetToken(" INVESTIDOR@EXAMPLE.COM ", "password123");

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Investidor"))
				.andExpect(jsonPath("$.email").value("investidor@example.com"))
				.andExpect(jsonPath("$.role").value("INVESTOR"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void returnsSameErrorForUnknownEmailAndWrongPassword() throws Exception {
		register("Investidor", "investidor@example.com", "password123");

		MvcResult unknownEmail = login("unknown@example.com", "password123");
		MvcResult wrongPassword = login("investidor@example.com", "wrong-password");
		JsonNode unknownBody = objectMapper.readTree(unknownEmail.getResponse().getContentAsString());
		JsonNode wrongBody = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());

		assertEquals(401, unknownEmail.getResponse().getStatus());
		assertEquals(401, wrongPassword.getResponse().getStatus());
		assertEquals(unknownBody.get("code"), wrongBody.get("code"));
		assertEquals(unknownBody.get("message"), wrongBody.get("message"));
	}

	@Test
	void rejectsMissingInvalidExpiredTokensAndInsufficientRoleAsJson() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.path").value("/api/auth/me"));

		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-token"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		String expiredToken = expiredInvestorToken();
		mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expiredToken))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		register("Investidor", "investidor@example.com", "password123");
		String investorToken = loginAndGetToken("investidor@example.com", "password123");
		mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + investorToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void returnsStandardJsonForMalformedBody() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{invalid-json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.path").value("/api/auth/register"))
				.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	private void register(String name, String email, String password) throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"name", name,
						"email", email,
						"password", password))))
				.andExpect(status().isCreated());
	}

	private MvcResult login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"email", email,
						"password", password))))
				.andReturn();
	}

	private String loginAndGetToken(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
						"email", email,
						"password", password))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresAt").exists())
				.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
	}

	private String expiredInvestorToken() {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(UUID.randomUUID().toString())
				.issuedAt(now.minusSeconds(7200))
				.expiresAt(now.minusSeconds(3600))
				.claim("role", "INVESTOR")
				.build();
		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();
	}
}
