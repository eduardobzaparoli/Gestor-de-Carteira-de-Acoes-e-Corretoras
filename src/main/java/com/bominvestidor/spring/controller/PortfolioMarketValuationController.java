package com.bominvestidor.spring.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.valuation.PortfolioMarketValuationResponse;
import com.bominvestidor.spring.service.valuation.PortfolioMarketValuationService;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/valuation")
@PreAuthorize("hasRole('INVESTOR')")
public class PortfolioMarketValuationController {
	private final PortfolioMarketValuationService service;
	public PortfolioMarketValuationController(PortfolioMarketValuationService service) { this.service = service; }

	@GetMapping
	public PortfolioMarketValuationResponse find(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId) {
		return service.find(UUID.fromString(jwt.getSubject()), portfolioId);
	}
}
