package com.bominvestidor.spring.domain.valuation;

import java.math.BigDecimal;
import java.util.List;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;

public record PortfolioConsolidatedSummary(String baseCurrency, BigDecimal investedValue, BigDecimal marketValue,
		BigDecimal totalGain, BigDecimal returnPercentage, List<ExchangeRate> exchangeRates,
		List<ExchangeRate> historicalExchangeRates) { }
