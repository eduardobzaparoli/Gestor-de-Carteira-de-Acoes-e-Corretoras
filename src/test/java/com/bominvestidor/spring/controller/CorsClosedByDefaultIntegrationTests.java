package com.bominvestidor.spring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsClosedByDefaultIntegrationTests {
	@Autowired MockMvc mockMvc;

	@Test void doesNotAuthorizeCrossOriginRequestsWithoutConfiguration() throws Exception {
		mockMvc.perform(options("/api/portfolios").header("Origin", "https://frontend.example")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
