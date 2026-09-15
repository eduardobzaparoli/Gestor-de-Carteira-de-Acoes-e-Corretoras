package com.bominvestidor.spring.service.exchange;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;

class ExchangeRateCacheTests {
	@Test
	void cachesByPairAndReferenceDateUntilExpiration() {
		MutableClock clock = new MutableClock(Instant.parse("2026-08-29T12:00:00Z"));
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setExchangeRateCacheTtl(Duration.ofMinutes(1));
		ExchangeRateCache cache = new ExchangeRateCache(clock, properties);
		ExchangeRate rate = new ExchangeRate("USD", "BRL", new BigDecimal("5.20"), LocalDate.of(2026, 8, 28));
		LocalDate requestedDate = LocalDate.of(2026, 8, 29);
		cache.store("USD", "BRL", requestedDate, rate);

		assertTrue(cache.find("USD", "BRL", requestedDate).isPresent());
		assertTrue(cache.find("USD", "BRL", LocalDate.of(2026, 8, 28)).isPresent());
		clock.advance(Duration.ofMinutes(1));
		assertTrue(cache.find("USD", "BRL", requestedDate).isEmpty());
	}

	private static final class MutableClock extends Clock {
		private Instant instant;
		private MutableClock(Instant instant) { this.instant = instant; }
		void advance(Duration duration) { instant = instant.plus(duration); }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return instant; }
	}
}
