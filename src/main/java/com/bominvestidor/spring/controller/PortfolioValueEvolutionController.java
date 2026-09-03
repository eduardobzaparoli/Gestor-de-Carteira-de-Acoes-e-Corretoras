package com.bominvestidor.spring.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.evolution.PortfolioValueEvolutionPointResponse;
import com.bominvestidor.spring.service.evolution.PortfolioValueEvolutionService;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/value-evolution")
@PreAuthorize("hasRole('INVESTOR')")
public class PortfolioValueEvolutionController {
	private final PortfolioValueEvolutionService service;
	public PortfolioValueEvolutionController(PortfolioValueEvolutionService service) { this.service = service; }
	@GetMapping
	public List<PortfolioValueEvolutionPointResponse> find(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId) {
		return service.find(UUID.fromString(jwt.getSubject()), portfolioId);
	}
}
