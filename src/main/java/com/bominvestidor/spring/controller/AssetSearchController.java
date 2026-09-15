package com.bominvestidor.spring.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bominvestidor.spring.dto.asset.AssetSearchResponse;
import com.bominvestidor.spring.service.asset.AssetSearchService;

@RestController
@RequestMapping("/api/assets")
@PreAuthorize("hasRole('INVESTOR')")
public class AssetSearchController {
	private final AssetSearchService assetSearchService;
	public AssetSearchController(AssetSearchService assetSearchService) { this.assetSearchService = assetSearchService; }

	@GetMapping("/search")
	public List<AssetSearchResponse> search(@AuthenticationPrincipal Jwt jwt,
			@RequestParam String market, @RequestParam String assetType, @RequestParam String query) {
		return assetSearchService.search(UUID.fromString(jwt.getSubject()), market, assetType, query);
	}
}
