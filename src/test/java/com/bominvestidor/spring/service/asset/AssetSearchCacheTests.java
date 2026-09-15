package com.bominvestidor.spring.service.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;

class AssetSearchCacheTests {
	@Test
	void expiresSearchesAfterFiveMinutesAndQuotesAfterOneMinute() {
		MutableClock clock = new MutableClock(Instant.parse("2026-08-29T12:00:00Z"));
		AssetSearchCache cache = new AssetSearchCache(clock, new BrokerageIntegrationProperties());
		AssetCandidate candidate = new AssetCandidate("PETR4", "Petrobras", AssetMarket.BR, AssetType.STOCK, "BRL");
		cache.storeCandidates(AssetMarket.BR, AssetType.STOCK, "PETR", List.of(candidate));
		cache.storeQuote(AssetMarket.BR, "PETR4", new AssetQuote("PETR4", "BRL", new BigDecimal("35.10")));

		clock.advance(Duration.ofMinutes(1));
		assertEquals("PETR4", cache.findCandidates(AssetMarket.BR, AssetType.STOCK, "PETR").orElseThrow().get(0).ticker());
		assertTrue(cache.findQuote(AssetMarket.BR, "PETR4").isEmpty());
		clock.advance(Duration.ofMinutes(4));
		assertTrue(cache.findCandidates(AssetMarket.BR, AssetType.STOCK, "PETR").isEmpty());
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
