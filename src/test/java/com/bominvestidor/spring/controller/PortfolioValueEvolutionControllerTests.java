package com.bominvestidor.spring.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.evolution.PortfolioValueEvolutionPointResponse;
import com.bominvestidor.spring.exception.HistoricalPriceUnavailableException;
import com.bominvestidor.spring.exception.PortfolioNotFoundException;
import com.bominvestidor.spring.service.evolution.PortfolioValueEvolutionService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioValueEvolutionControllerTests {
	@Autowired private MockMvc mockMvc;
	@MockitoBean private PortfolioValueEvolutionService service;
	private final UUID ownerId = UUID.randomUUID();
	private final UUID portfolioId = UUID.randomUUID();

	@Test
	void returnsOrderedJsonPointsAndEmptyCollection() throws Exception {
		when(service.find(ownerId, portfolioId)).thenReturn(List.of(
				new PortfolioValueEvolutionPointResponse(LocalDate.of(2026, 9, 2), new BigDecimal("81"), new BigDecimal("96.40")),
				new PortfolioValueEvolutionPointResponse(LocalDate.of(2026, 9, 3), new BigDecimal("81"), new BigDecimal("95.06"))));

		mockMvc.perform(get(path(portfolioId)).with(investor(ownerId)))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$[0].date").value("2026-09-02"))
				.andExpect(jsonPath("$[0].investedValue").value(81))
				.andExpect(jsonPath("$[1].marketValue").value(95.06));

		UUID emptyPortfolio = UUID.randomUUID();
		when(service.find(ownerId, emptyPortfolio)).thenReturn(List.of());
		mockMvc.perform(get(path(emptyPortfolio)).with(investor(ownerId)))
				.andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void returnsJson401WithoutTokenAnd403ForAdministrator() throws Exception {
		mockMvc.perform(get(path(portfolioId)))
				.andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		Jwt admin = Jwt.withTokenValue("admin-token").subject(UUID.randomUUID().toString())
				.claim("role", UserRole.ADMIN.name()).header("alg", "none").build();
		mockMvc.perform(get(path(portfolioId)).with(jwt().jwt(admin)))
				.andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void returnsIndistinguishable404ForUnavailablePortfolio() throws Exception {
		when(service.find(ownerId, portfolioId)).thenThrow(new PortfolioNotFoundException());

		mockMvc.perform(get(path(portfolioId)).with(investor(ownerId)))
				.andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Portfolio was not found"));
	}

	@Test
	void returnsAtomicSanitized503WithoutInternalOrPartialData() throws Exception {
		when(service.find(ownerId, portfolioId)).thenThrow(new HistoricalPriceUnavailableException());

		mockMvc.perform(get(path(portfolioId)).with(investor(ownerId)))
				.andExpect(status().isServiceUnavailable()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("HISTORICAL_PRICE_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("Historical market prices are unavailable"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty())
				.andExpect(jsonPath("$.points").doesNotExist())
				.andExpect(jsonPath("$.providerPayload").doesNotExist());
	}

	private String path(UUID id) { return "/api/portfolios/" + id + "/value-evolution"; }

	private org.springframework.test.web.servlet.request.RequestPostProcessor investor(UUID id) {
		Jwt token = Jwt.withTokenValue("investor-token").subject(id.toString())
				.claim("role", UserRole.INVESTOR.name()).header("alg", "none").build();
		return jwt().jwt(token).authorities(new SimpleGrantedAuthority("ROLE_INVESTOR"));
	}
}
