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

import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;
import com.bominvestidor.spring.service.position.PortfolioPositionService;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/positions")
@PreAuthorize("hasRole('INVESTOR')")
public class PortfolioPositionController {
	private final PortfolioPositionService service;

	public PortfolioPositionController(PortfolioPositionService service) { this.service = service; }

	@GetMapping
	public List<PortfolioPositionResponse> findAll(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId) {
		return service.findAll(UUID.fromString(jwt.getSubject()), portfolioId);
	}
}
