package com.bominvestidor.spring.service.income;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventType;
import com.bominvestidor.spring.domain.income.IncomeProviderEvent;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategy;

class IncomeProviderEventCacheTests {
	@Test
	void cachesEmptyResponsesAndDeduplicatesConcurrentRequests() throws Exception {
		MutableClock clock = new MutableClock();
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		IncomeProviderEventCache cache = new IncomeProviderEventCache(clock, properties);
		AtomicInteger calls = new AtomicInteger();
		IncomeEventProviderStrategy provider = provider(ticker -> { calls.incrementAndGet(); return List.of(); });
		CountDownLatch start = new CountDownLatch(1);
		var executor = Executors.newFixedThreadPool(2);
		try {
			var first = executor.submit(() -> { start.await(); return cache.resolve(AssetMarket.US, Set.of("MSFT"), provider); });
			var second = executor.submit(() -> { start.await(); return cache.resolve(AssetMarket.US, Set.of("MSFT"), provider); });
			start.countDown();
			assertThat(first.get().events().get("MSFT")).isEmpty();
			assertThat(second.get().events().get("MSFT")).isEmpty();
		} finally { executor.shutdownNow(); }
		assertThat(calls).hasValue(1);
	}

	@Test
	void usesStaleDataAfterRefreshFailureAndFailsAfterContingency() {
		MutableClock clock = new MutableClock();
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		IncomeProviderEventCache cache = new IncomeProviderEventCache(clock, properties);
		AtomicInteger calls = new AtomicInteger();
		IncomeEventProviderStrategy provider = provider(ticker -> {
			if (calls.incrementAndGet() > 1) throw new AssetProviderUnavailableException("ALPHAVANTAGE_RATE_LIMITED", "limited");
			return List.of(event(ticker));
		});
		cache.resolve(AssetMarket.US, Set.of("MSFT"), provider);
		clock.advance(java.time.Duration.ofMinutes(31));
		var stale = cache.resolve(AssetMarket.US, Set.of("MSFT"), provider);
		assertThat(stale.stale()).isTrue();
		assertThat(stale.events().get("MSFT")).hasSize(1);
		assertThat(stale.warnings()).singleElement().extracting("code").isEqualTo("ALPHAVANTAGE_RATE_LIMITED");
		clock.advance(java.time.Duration.ofHours(7));
		assertThatThrownBy(() -> cache.resolve(AssetMarket.US, Set.of("MSFT"), provider))
				.isInstanceOf(AssetProviderUnavailableException.class)
				.hasMessageContaining("No reliable");
	}

	@Test
	void preservesSuccessfulTickersAndReportsPartialFailures() {
		MutableClock clock = new MutableClock();
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		IncomeProviderEventCache cache = new IncomeProviderEventCache(clock, properties);
		IncomeEventProviderStrategy provider = provider(ticker -> {
			if (ticker.equals("AAPL")) {
				throw new AssetProviderUnavailableException("ALPHAVANTAGE_RATE_LIMITED", "limited");
			}
			return List.of(event(ticker));
		});

		var resolution = cache.resolve(AssetMarket.US, Set.of("MSFT", "AAPL"), provider);

		assertThat(resolution.events().get("MSFT")).hasSize(1);
		assertThat(resolution.events()).doesNotContainKey("AAPL");
		assertThat(resolution.stale()).isFalse();
		assertThat(resolution.warnings()).singleElement()
				.extracting("ticker", "code")
				.containsExactly("AAPL", "ALPHAVANTAGE_RATE_LIMITED");
	}

	private IncomeEventProviderStrategy provider(java.util.function.Function<String, List<IncomeProviderEvent>> function) {
		return new IncomeEventProviderStrategy() {
			@Override public AssetMarket market() { return AssetMarket.US; }
			@Override public List<IncomeProviderEvent> findEvents(String ticker) { return function.apply(ticker); }
		};
	}
	private IncomeProviderEvent event(String ticker) {
		return new IncomeProviderEvent("test|" + ticker, IncomeEventType.DIVIDEND, BigDecimal.ONE,
				LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), IncomeEventSource.ALPHA_VANTAGE);
	}
	private static final class MutableClock extends Clock {
		private Instant now = Instant.parse("2026-09-12T12:00:00Z");
		void advance(java.time.Duration duration) { now = now.plus(duration); }
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
	}
}
