package com.bominvestidor.spring.dto.valuation;

import java.math.BigDecimal;

public record PortfolioCurrencySummaryResponse(String currency, BigDecimal investedValue, BigDecimal marketValue,
		BigDecimal unrealizedGain, BigDecimal returnPercentage) { }
