package com.bominvestidor.spring.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionCreateRequest;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionResponse;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionUpdateRequest;
import com.bominvestidor.spring.service.transaction.PortfolioTransactionService;
import jakarta.validation.Valid;

@RestController @RequestMapping("/api/portfolios/{portfolioId}/transactions") @PreAuthorize("hasRole('INVESTOR')")
public class PortfolioTransactionController {
	private final PortfolioTransactionService service;
	public PortfolioTransactionController(PortfolioTransactionService service) { this.service = service; }
	@PostMapping public ResponseEntity<PortfolioTransactionResponse> create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId,
			@Valid @RequestBody PortfolioTransactionCreateRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(ownerId(jwt), portfolioId, request)); }
	@GetMapping public List<PortfolioTransactionResponse> findAll(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId) { return service.findAll(ownerId(jwt), portfolioId); }
	@DeleteMapping("/{transactionId}") public ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID portfolioId, @PathVariable UUID transactionId) { service.cancel(ownerId(jwt), portfolioId, transactionId); return ResponseEntity.noContent().build(); }
	@PutMapping("/{transactionId}") public PortfolioTransactionResponse update(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID portfolioId, @PathVariable UUID transactionId,
			@Valid @RequestBody PortfolioTransactionUpdateRequest request) {
		return service.update(ownerId(jwt), portfolioId, transactionId, request);
	}
	private UUID ownerId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
