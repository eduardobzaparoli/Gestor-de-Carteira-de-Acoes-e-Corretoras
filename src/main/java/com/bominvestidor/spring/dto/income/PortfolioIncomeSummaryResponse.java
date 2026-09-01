package com.bominvestidor.spring.dto.income;

import java.math.BigDecimal;
import java.util.List;

import com.bominvestidor.spring.dto.valuation.ExchangeRateResponse;

public record PortfolioIncomeSummaryResponse(List<PortfolioIncomeCurrencySummaryResponse> currencySummaries,
		String baseCurrency, BigDecimal consolidatedReceivedAmount, List<ExchangeRateResponse> exchangeRates) { }
