package com.bominvestidor.spring.integration.exchange;

import java.time.LocalDate;
import java.util.Optional;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;

public interface ExchangeRateStrategy {
	Optional<ExchangeRate> findClosingRate(String sourceCurrency, String targetCurrency, LocalDate requestedDate);
}
