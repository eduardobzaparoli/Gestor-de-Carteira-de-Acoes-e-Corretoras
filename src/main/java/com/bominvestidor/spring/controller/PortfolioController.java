package com.bominvestidor.spring.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.portfolio.PortfolioCreateRequest;
import com.bominvestidor.spring.dto.portfolio.PortfolioResponse;
import com.bominvestidor.spring.dto.portfolio.PortfolioUpdateRequest;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/portfolios")
@PreAuthorize("hasRole('INVESTOR')")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@PostMapping
	public ResponseEntity<PortfolioResponse> create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PortfolioCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.create(ownerId(jwt), request));
	}

	@GetMapping
	public List<PortfolioResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
		return portfolioService.findAll(ownerId(jwt));
	}

	@GetMapping("/{id}")
	public PortfolioResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return portfolioService.findById(ownerId(jwt), id);
	}

	@PutMapping("/{id}")
	public PortfolioResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
			@Valid @RequestBody PortfolioUpdateRequest request) {
		return portfolioService.update(ownerId(jwt), id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		portfolioService.delete(ownerId(jwt), id);
		return ResponseEntity.noContent().build();
	}

	private UUID ownerId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
