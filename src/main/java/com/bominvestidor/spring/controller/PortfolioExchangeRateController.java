package com.bominvestidor.spring.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.valuation.ExchangeRateResponse;
import com.bominvestidor.spring.service.exchange.PortfolioExchangeRateService;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/exchange-rates")
@PreAuthorize("hasRole('INVESTOR')")
public class PortfolioExchangeRateController {

	private final PortfolioExchangeRateService service;

	public PortfolioExchangeRateController(PortfolioExchangeRateService service) {
		this.service = service;
	}

	@GetMapping
	public ExchangeRateResponse find(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId,
			@RequestParam String sourceCurrency,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return service.find(UUID.fromString(jwt.getSubject()), portfolioId, sourceCurrency, date);
	}
}
