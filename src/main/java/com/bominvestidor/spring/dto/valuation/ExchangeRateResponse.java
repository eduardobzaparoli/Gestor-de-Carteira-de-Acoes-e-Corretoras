package com.bominvestidor.spring.dto.valuation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(String sourceCurrency, String targetCurrency, BigDecimal rate, LocalDate referenceDate) { }
