package com.bominvestidor.spring.dto.income;

import java.math.BigDecimal;

public record PortfolioIncomeCurrencySummaryResponse(String currency, BigDecimal receivedAmount) { }
