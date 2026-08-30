package com.bominvestidor.spring.service.valuation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;
import com.bominvestidor.spring.exception.AssetQuoteUnavailableException;
import com.bominvestidor.spring.mapper.valuation.PortfolioMarketValuationMapper;
import com.bominvestidor.spring.service.asset.AssetSearchService;
import com.bominvestidor.spring.service.position.PortfolioPositionService;

@ExtendWith(MockitoExtension.class)
class PortfolioMarketValuationServiceTests {
	@Mock private PortfolioPositionService positions;
	@Mock private AssetSearchService assets;
	private final UUID ownerId = UUID.randomUUID();
	private final UUID portfolioId = UUID.randomUUID();

	@Test
	void failsWithoutPartialResponseWhenAnyQuoteIsMissing() {
		PortfolioPositionResponse petr = position("PETR4", AssetMarket.BR, "BRL");
		PortfolioPositionResponse msft = position("MSFT", AssetMarket.US, "USD");
		when(positions.findAll(ownerId, portfolioId)).thenReturn(List.of(petr, msft));
		when(assets.findQuote(AssetMarket.BR, "PETR4")).thenReturn(Optional.of(new AssetQuote("PETR4", "BRL", new BigDecimal("12"))));
		when(assets.findQuote(AssetMarket.US, "MSFT")).thenReturn(Optional.empty());

		assertThrows(AssetQuoteUnavailableException.class, () -> service().find(ownerId, portfolioId));
	}

	@Test
	void returnsValuationWhenAllQuotesAreCompatible() {
		PortfolioPositionResponse petr = position("PETR4", AssetMarket.BR, "BRL");
		when(positions.findAll(ownerId, portfolioId)).thenReturn(List.of(petr));
		when(assets.findQuote(AssetMarket.BR, "PETR4")).thenReturn(Optional.of(new AssetQuote("PETR4", "BRL", new BigDecimal("12"))));

		var response = service().find(ownerId, portfolioId);
		assertEquals(1, response.positions().size());
		assertEquals(0, new BigDecimal("120").compareTo(response.positions().get(0).marketValue()));
		assertEquals(1, response.currencySummaries().size());
	}

	private PortfolioMarketValuationService service() { return new PortfolioMarketValuationService(positions, assets, new PortfolioMarketValuationMapper()); }
	private PortfolioPositionResponse position(String ticker, AssetMarket market, String currency) {
		return new PortfolioPositionResponse(ticker, ticker, market, AssetType.STOCK, currency, new BigDecimal("10"),
				new BigDecimal("10"), new BigDecimal("100"));
	}
}
