package com.bominvestidor.spring.domain.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRate(String sourceCurrency, String targetCurrency, BigDecimal rate, LocalDate referenceDate) { }
