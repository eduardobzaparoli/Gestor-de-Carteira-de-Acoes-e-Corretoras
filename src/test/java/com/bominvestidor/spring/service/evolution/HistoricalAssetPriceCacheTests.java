package com.bominvestidor.spring.service.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetKey;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPrice;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;

class HistoricalAssetPriceCacheTests {
	@Test
	void normalizesTickerSeparatesIntervalsAndExpiresAtConfiguredTtl() {
		MutableClock clock = new MutableClock(Instant.parse("2026-09-03T12:00:00Z"));
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setHistoricalPriceCacheTtl(Duration.ofMinutes(15));
		HistoricalAssetPriceCache cache = new HistoricalAssetPriceCache(properties, clock);
		LocalDate from = LocalDate.of(2026, 8, 27);
		LocalDate to = LocalDate.of(2026, 9, 2);
		HistoricalAssetPriceSeries series = series(from);

		cache.store(AssetMarket.US, " msft ", from, to, series);

		assertEquals(series, cache.find(AssetMarket.US, "MSFT", from, to).orElseThrow());
		assertTrue(cache.find(AssetMarket.US, "MSFT", from.plusDays(1), to).isEmpty());
		clock.advance(Duration.ofMinutes(14).plusSeconds(59));
		assertTrue(cache.find(AssetMarket.US, "msft", from, to).isPresent());
		clock.advance(Duration.ofSeconds(1));
		assertTrue(cache.find(AssetMarket.US, "MSFT", from, to).isEmpty());
	}

	private HistoricalAssetPriceSeries series(LocalDate date) {
		TreeMap<LocalDate, HistoricalAssetPrice> prices = new TreeMap<>();
		prices.put(date, new HistoricalAssetPrice(date, new BigDecimal("500.10")));
		return new HistoricalAssetPriceSeries(new HistoricalAssetKey(AssetMarket.US, "MSFT", "USD"), prices);
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
