package com.bominvestidor.spring.domain.income;

import java.math.BigDecimal;

public record PortfolioIncomeCurrencySummary(String currency, BigDecimal receivedAmount) { }
