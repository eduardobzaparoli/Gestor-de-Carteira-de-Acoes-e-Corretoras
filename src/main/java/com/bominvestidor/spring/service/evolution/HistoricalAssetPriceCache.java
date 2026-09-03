package com.bominvestidor.spring.service.evolution;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;

@Component
public class HistoricalAssetPriceCache {
	private final ConcurrentHashMap<Key, Entry> entries = new ConcurrentHashMap<>();
	private final BrokerageIntegrationProperties properties;
	private final Clock clock;
	public HistoricalAssetPriceCache(BrokerageIntegrationProperties properties, Clock clock) { this.properties = properties; this.clock = clock; }
	public Optional<HistoricalAssetPriceSeries> find(AssetMarket market, String ticker, LocalDate from, LocalDate to) {
		Entry entry = entries.get(new Key(market, ticker, from, to));
		if (entry == null || !entry.expiresAt().isAfter(clock.instant())) return Optional.empty();
		return Optional.of(entry.series());
	}
	public void store(AssetMarket market, String ticker, LocalDate from, LocalDate to, HistoricalAssetPriceSeries series) {
		entries.put(new Key(market, ticker, from, to), new Entry(series, clock.instant().plus(properties.getHistoricalPriceCacheTtl())));
	}
	private record Key(AssetMarket market, String ticker, LocalDate from, LocalDate to) {
		private Key { ticker = ticker.trim().toUpperCase(Locale.ROOT); }
	}
	private record Entry(HistoricalAssetPriceSeries series, Instant expiresAt) { }
}
