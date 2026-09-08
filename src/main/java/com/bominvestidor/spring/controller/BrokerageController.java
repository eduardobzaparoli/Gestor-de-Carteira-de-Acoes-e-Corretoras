package com.bominvestidor.spring.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.brokerage.BrokerageCreateRequest;
import com.bominvestidor.spring.dto.brokerage.BrokerageResponse;
import com.bominvestidor.spring.dto.brokerage.CepLookupResponse;
import com.bominvestidor.spring.dto.brokerage.CnpjLookupResponse;
import com.bominvestidor.spring.service.brokerage.BrokerageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/brokerages")
@PreAuthorize("hasRole('INVESTOR')")
public class BrokerageController {

	private final BrokerageService brokerageService;

	public BrokerageController(BrokerageService brokerageService) {
		this.brokerageService = brokerageService;
	}

	@GetMapping("/cep/{cep}")
	public CepLookupResponse lookupCep(@AuthenticationPrincipal Jwt jwt, @PathVariable String cep) {
		return brokerageService.lookupCep(ownerId(jwt), cep);
	}

	@GetMapping("/cep")
	public CepLookupResponse lookupCepByQuery(@AuthenticationPrincipal Jwt jwt, @RequestParam String cep) {
		return brokerageService.lookupCep(ownerId(jwt), cep);
	}

	@GetMapping("/cnpj/{cnpj}")
	public CnpjLookupResponse lookupCnpj(@AuthenticationPrincipal Jwt jwt, @PathVariable String cnpj) {
		return brokerageService.lookupCnpj(ownerId(jwt), cnpj);
	}

	@GetMapping("/cnpj")
	public CnpjLookupResponse lookupCnpjByQuery(@AuthenticationPrincipal Jwt jwt, @RequestParam String cnpj) {
		return brokerageService.lookupCnpj(ownerId(jwt), cnpj);
	}

	@PostMapping
	public ResponseEntity<BrokerageResponse> register(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody BrokerageCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(brokerageService.register(ownerId(jwt), request));
	}

	@GetMapping
	public List<BrokerageResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
		return brokerageService.findAll(ownerId(jwt));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		brokerageService.delete(ownerId(jwt), id);
		return ResponseEntity.noContent().build();
	}

	private UUID ownerId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
