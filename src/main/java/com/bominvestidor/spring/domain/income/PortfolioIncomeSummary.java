package com.bominvestidor.spring.domain.income;

import java.math.BigDecimal;
import java.util.List;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;

public record PortfolioIncomeSummary(List<PortfolioIncomeCurrencySummary> currencySummaries,
		String baseCurrency, BigDecimal consolidatedReceivedAmount, List<ExchangeRate> exchangeRates) { }
