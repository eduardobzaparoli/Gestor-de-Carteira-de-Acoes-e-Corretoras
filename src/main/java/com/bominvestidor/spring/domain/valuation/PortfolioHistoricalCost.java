package com.bominvestidor.spring.domain.valuation;

import java.math.BigDecimal;
import java.util.List;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;

public record PortfolioHistoricalCost(BigDecimal investedValue, List<ExchangeRate> exchangeRates) { }
