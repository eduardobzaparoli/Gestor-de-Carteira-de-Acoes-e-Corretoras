package com.bominvestidor.spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest(properties = "app.web.cors.allowed-origins=https://frontend.example")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiOperationsIntegrationTests {
	@Autowired MockMvc mockMvc;

	@Test void exposesSafeOpenApiAndHealthWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
				.andExpect(jsonPath("$.components.schemas.ApiError").exists())
				.andExpect(jsonPath("$.paths['/api/portfolios'].get.responses['401']").exists())
				.andExpect(jsonPath("$.paths['/api/portfolios'].get.responses['503']").exists())
				.andExpect(jsonPath("$.paths['/api/auth/login'].post.security").isEmpty());
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
		mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
	}

	@Test void permitsOnlyConfiguredCorsOrigin() throws Exception {
		mockMvc.perform(options("/api/portfolios").header("Origin", "https://frontend.example")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "https://frontend.example"));
		mockMvc.perform(options("/api/portfolios").header("Origin", "https://evil.example")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
