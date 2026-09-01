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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.dto.income.IncomeEventCandidateResponse;
import com.bominvestidor.spring.dto.income.IncomeEventConfirmationRequest;
import com.bominvestidor.spring.dto.income.ManualIncomeEventCreateRequest;
import com.bominvestidor.spring.dto.income.PortfolioIncomeEventResponse;
import com.bominvestidor.spring.dto.income.PortfolioIncomeSummaryResponse;
import com.bominvestidor.spring.service.income.PortfolioIncomeEventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/income-events")
@PreAuthorize("hasRole('INVESTOR')")
public class PortfolioIncomeEventController {
	private final PortfolioIncomeEventService service;
	public PortfolioIncomeEventController(PortfolioIncomeEventService service) { this.service = service; }

	@GetMapping("/candidates")
	public List<IncomeEventCandidateResponse> candidates(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId,
			@RequestParam AssetMarket market) { return service.findCandidates(ownerId(jwt), portfolioId, market); }
	@PostMapping("/confirmations")
	public ResponseEntity<PortfolioIncomeEventResponse> confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId,
			@Valid @RequestBody IncomeEventConfirmationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.confirm(ownerId(jwt), portfolioId, request));
	}
	@PostMapping("/manual")
	public ResponseEntity<PortfolioIncomeEventResponse> manual(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId,
			@Valid @RequestBody ManualIncomeEventCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createManual(ownerId(jwt), portfolioId, request));
	}
	@GetMapping public List<PortfolioIncomeEventResponse> findAll(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId) {
		return service.findAll(ownerId(jwt), portfolioId);
	}
	@GetMapping("/summary") public PortfolioIncomeSummaryResponse summary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId) {
		return service.summary(ownerId(jwt), portfolioId);
	}
	@DeleteMapping("/{incomeEventId}") public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId,
			@PathVariable UUID incomeEventId) { service.cancel(ownerId(jwt), portfolioId, incomeEventId); return ResponseEntity.noContent().build(); }
	private UUID ownerId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
