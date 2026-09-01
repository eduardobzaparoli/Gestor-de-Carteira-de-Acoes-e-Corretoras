package com.bominvestidor.spring.dto.valuation;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record PortfolioMarketValuationResponse(List<PortfolioValuationPositionResponse> positions,
		List<PortfolioCurrencySummaryResponse> currencySummaries,
		@JsonInclude(JsonInclude.Include.NON_NULL) PortfolioConsolidatedSummaryResponse consolidatedSummary) { }
