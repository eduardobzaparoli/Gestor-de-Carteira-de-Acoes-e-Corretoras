package com.bominvestidor.spring.domain.valuation;

import java.util.List;

public record PortfolioMarketValuation(List<PortfolioValuationPosition> positions,
		List<PortfolioCurrencySummary> currencySummaries) { }
