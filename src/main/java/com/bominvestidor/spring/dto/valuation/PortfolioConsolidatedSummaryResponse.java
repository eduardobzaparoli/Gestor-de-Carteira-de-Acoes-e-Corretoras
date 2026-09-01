package com.bominvestidor.spring.dto.valuation;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioConsolidatedSummaryResponse(String baseCurrency, BigDecimal investedValue, BigDecimal marketValue,
		BigDecimal totalGain, BigDecimal returnPercentage, List<ExchangeRateResponse> exchangeRates,
		List<ExchangeRateResponse> historicalExchangeRates) { }
