package com.bominvestidor.spring.domain.valuation;

import java.math.BigDecimal;

public record PortfolioCurrencySummary(String currency, BigDecimal investedValue, BigDecimal marketValue,
		BigDecimal unrealizedGain, BigDecimal returnPercentage) { }
