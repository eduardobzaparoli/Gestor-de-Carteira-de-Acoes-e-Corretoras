package com.bominvestidor.spring.service.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.exception.InvalidAssetSearchDataException;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategy;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

@ExtendWith(MockitoExtension.class)
class AssetSearchServiceTests {
	@Mock private PortfolioService portfolioService;
	@Mock private AssetSearchStrategy strategy;
	private AssetSearchService service;
	private final UUID ownerId = UUID.randomUUID();
	private final UUID portfolioId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		when(strategy.market()).thenReturn(AssetMarket.BR);
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		AssetSearchCache cache = new AssetSearchCache(Clock.fixed(Instant.parse("2026-08-28T12:00:00Z"), ZoneOffset.UTC), properties);
		service = new AssetSearchService(portfolioService, new AssetSearchStrategyResolver(List.of(strategy)), cache);
	}

	@Test
	void returnsOnlyQuotedCandidatesAndCachesSearchAndQuote() {
		AssetCandidate quoted = new AssetCandidate("PETR4", "Petrobras PN", AssetMarket.BR, AssetType.STOCK, "BRL");
		AssetCandidate unquoted = new AssetCandidate("VALE3", "Vale ON", AssetMarket.BR, AssetType.STOCK, "BRL");
		when(strategy.findCandidates(AssetType.STOCK, "PET")).thenReturn(List.of(quoted, unquoted));
		when(strategy.findQuote("PETR4")).thenReturn(Optional.of(new AssetQuote("PETR4", "BRL", new BigDecimal("35.10"))));
		when(strategy.findQuote("VALE3")).thenReturn(Optional.empty());

		var first = service.search(ownerId, portfolioId, "br", "stock", " pet ");
		var second = service.search(ownerId, portfolioId, "BR", "STOCK", "PET");

		assertEquals(1, first.size());
		assertEquals("PETR4", first.get(0).ticker());
		assertEquals(first, second);
		verify(portfolioService, times(2)).requireOwnedPortfolio(ownerId, portfolioId);
		verify(strategy, times(1)).findCandidates(AssetType.STOCK, "PET");
		verify(strategy, times(1)).findQuote("PETR4");
	}

	@Test
	void validatesSearchParametersAfterCheckingPortfolioAccess() {
		assertThrows(InvalidAssetSearchDataException.class,
				() -> service.search(ownerId, portfolioId, "BR", "ETF", " "));
		verify(portfolioService).requireOwnedPortfolio(ownerId, portfolioId);
	}
}
