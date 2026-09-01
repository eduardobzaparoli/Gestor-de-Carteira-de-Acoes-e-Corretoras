package com.bominvestidor.spring.service.exchange;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;

@Component
public class ExchangeRateCache {
	private final Map<ExchangeRateKey, TimedValue<ExchangeRate>> rates = new ConcurrentHashMap<>();
	private final Clock clock;
	private final java.time.Duration ttl;

	public ExchangeRateCache(Clock clock, BrokerageIntegrationProperties properties) {
		this.clock = clock;
		this.ttl = properties.getExchangeRateCacheTtl();
	}

	public Optional<ExchangeRate> find(String sourceCurrency, String targetCurrency, LocalDate referenceDate) {
		ExchangeRateKey key = new ExchangeRateKey(sourceCurrency, targetCurrency, referenceDate);
		TimedValue<ExchangeRate> value = rates.get(key);
		if (value == null) return Optional.empty();
		if (!clock.instant().isBefore(value.expiresAt())) {
			rates.remove(key, value);
			return Optional.empty();
		}
		return Optional.of(value.value());
	}

	public void store(String sourceCurrency, String targetCurrency, LocalDate requestedDate, ExchangeRate rate) {
		TimedValue<ExchangeRate> value = new TimedValue<>(rate, clock.instant().plus(ttl));
		rates.put(new ExchangeRateKey(sourceCurrency, targetCurrency, requestedDate), value);
		rates.put(new ExchangeRateKey(sourceCurrency, targetCurrency, rate.referenceDate()), value);
	}

	private record ExchangeRateKey(String sourceCurrency, String targetCurrency, LocalDate referenceDate) { }
	private record TimedValue<T>(T value, Instant expiresAt) { }
}
