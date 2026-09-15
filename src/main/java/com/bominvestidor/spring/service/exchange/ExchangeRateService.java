package com.bominvestidor.spring.service.exchange;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.integration.exchange.ExchangeRateStrategy;

@Service
public class ExchangeRateService {
	private final ExchangeRateStrategy strategy;
	private final ExchangeRateCache cache;

	public ExchangeRateService(ExchangeRateStrategy strategy, ExchangeRateCache cache) {
		this.strategy = strategy;
		this.cache = cache;
	}

	public Optional<ExchangeRate> find(String sourceCurrency, String targetCurrency, LocalDate requestedDate) {
		return cache.find(sourceCurrency, targetCurrency, requestedDate)
				.or(() -> strategy.findClosingRate(sourceCurrency, targetCurrency, requestedDate).map(rate -> {
					cache.store(sourceCurrency, targetCurrency, requestedDate, rate);
					return rate;
				}));
	}
}
