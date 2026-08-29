package com.bominvestidor.spring.service.asset;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;

@Component
public class AssetSearchCache {
	private final Map<SearchKey, TimedValue<List<AssetCandidate>>> candidates = new ConcurrentHashMap<>();
	private final Map<QuoteKey, TimedValue<AssetQuote>> quotes = new ConcurrentHashMap<>();
	private final Clock clock;
	private final Duration searchTtl;
	private final Duration quoteTtl;

	public AssetSearchCache(Clock clock, BrokerageIntegrationProperties properties) {
		this.clock = clock;
		this.searchTtl = properties.getAssetSearchCacheTtl();
		this.quoteTtl = properties.getAssetQuoteCacheTtl();
	}

	public Optional<List<AssetCandidate>> findCandidates(AssetMarket market, AssetType type, String query) {
		return find(candidates, new SearchKey(market, type, query));
	}
	public void storeCandidates(AssetMarket market, AssetType type, String query, List<AssetCandidate> value) {
		candidates.put(new SearchKey(market, type, query), new TimedValue<>(List.copyOf(value), expiresAt(searchTtl)));
	}
	public Optional<AssetQuote> findQuote(AssetMarket market, String ticker) {
		return find(quotes, new QuoteKey(market, ticker));
	}
	public void storeQuote(AssetMarket market, String ticker, AssetQuote value) {
		quotes.put(new QuoteKey(market, ticker), new TimedValue<>(value, expiresAt(quoteTtl)));
	}

	private Instant expiresAt(Duration ttl) { return clock.instant().plus(ttl); }
	private <K, T> Optional<T> find(Map<K, TimedValue<T>> values, K key) {
		TimedValue<T> value = values.get(key);
		if (value == null) return Optional.empty();
		if (!clock.instant().isBefore(value.expiresAt())) { values.remove(key, value); return Optional.empty(); }
		return Optional.of(value.value());
	}
	private record SearchKey(AssetMarket market, AssetType type, String query) { }
	private record QuoteKey(AssetMarket market, String ticker) { }
	private record TimedValue<T>(T value, Instant expiresAt) { }
}
