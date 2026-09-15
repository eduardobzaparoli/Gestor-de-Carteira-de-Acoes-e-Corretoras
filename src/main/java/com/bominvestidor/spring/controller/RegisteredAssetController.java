package com.bominvestidor.spring.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.bominvestidor.spring.dto.asset.RegisteredAssetCreateRequest;
import com.bominvestidor.spring.dto.asset.RegisteredAssetQuoteResponse;
import com.bominvestidor.spring.dto.asset.RegisteredAssetResponse;
import com.bominvestidor.spring.dto.valuation.ExchangeRateResponse;
import com.bominvestidor.spring.service.asset.RegisteredAssetService;
import com.bominvestidor.spring.service.exchange.PortfolioExchangeRateService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assets")
@PreAuthorize("hasRole('INVESTOR')")
public class RegisteredAssetController {
	private final RegisteredAssetService service;
	private final PortfolioExchangeRateService exchangeRateService;

	public RegisteredAssetController(RegisteredAssetService service, PortfolioExchangeRateService exchangeRateService) {
		this.service = service;
		this.exchangeRateService = exchangeRateService;
	}

	@PostMapping
	public ResponseEntity<RegisteredAssetResponse> register(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody RegisteredAssetCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.register(ownerId(jwt), request));
	}

	@GetMapping
	public List<RegisteredAssetResponse> findAll(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String market) {
		return service.findAll(ownerId(jwt), market);
	}

	@GetMapping("/exchange-rate")
	public ExchangeRateResponse exchangeRate(@RequestParam String sourceCurrency,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		return exchangeRateService.find(sourceCurrency, date);
	}

	@PostMapping("/{assetId}/quote-refresh")
	public RegisteredAssetResponse refreshQuote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID assetId) {
		return service.refreshQuote(ownerId(jwt), assetId);
	}

	@GetMapping("/{assetId}/quote")
	public RegisteredAssetQuoteResponse freshQuote(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID assetId) {
		return service.freshQuote(ownerId(jwt), assetId);
	}

	@DeleteMapping("/{assetId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID assetId) {
		service.delete(ownerId(jwt), assetId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	public ResponseEntity<Void> deleteAll(@AuthenticationPrincipal Jwt jwt) {
		service.deleteAll(ownerId(jwt));
		return ResponseEntity.noContent().build();
	}

	private UUID ownerId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
