package com.bominvestidor.spring.dto.valuation;

import java.util.List;

public record PortfolioMarketValuationResponse(List<PortfolioValuationPositionResponse> positions,
		List<PortfolioCurrencySummaryResponse> currencySummaries) { }
