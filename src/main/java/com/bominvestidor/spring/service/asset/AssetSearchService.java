package com.bominvestidor.spring.service.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.SelectedAsset;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.dto.asset.AssetSearchResponse;
import com.bominvestidor.spring.dto.error.FieldErrorResponse;
import com.bominvestidor.spring.exception.InvalidAssetSearchDataException;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategy;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;

@Service
public class AssetSearchService {
	private static final int MAX_RESULTS = 5;
	private final AssetSearchStrategyResolver strategyResolver;
	private final AssetSearchCache cache;
	private final AssetSelectionCache selectionCache;

	public AssetSearchService(AssetSearchStrategyResolver strategyResolver, AssetSearchCache cache,
			AssetSelectionCache selectionCache) {
		this.strategyResolver = strategyResolver;
		this.cache = cache;
		this.selectionCache = selectionCache;
	}

	public List<AssetSearchResponse> search(UUID ownerId, String marketValue, String typeValue,
			String queryValue) {
		AssetMarket market = market(marketValue);
		AssetType type = type(typeValue);
		String query = query(queryValue);
		AssetSearchStrategy strategy = strategyResolver.resolve(market);
		List<AssetCandidate> candidates = cache.findCandidates(market, type, query)
				.orElseGet(() -> findAndCache(strategy, market, type, query));
		List<AssetSearchResponse> results = new ArrayList<>();
		for (AssetCandidate candidate : candidates) {
			if (results.size() == MAX_RESULTS) break;
			cache.findQuote(market, candidate.ticker()).or(() -> findAndCacheQuote(strategy, market, candidate.ticker()))
					.filter(quote -> AssetIdentityRules.hasCompatibleCurrency(market, quote.currency()))
					.ifPresent(quote -> results.add(toResponse(ownerId, candidate, quote)));
		}
		return List.copyOf(results);
	}

	public java.util.Optional<AssetQuote> findQuote(AssetMarket market, String ticker) {
		AssetSearchStrategy strategy = strategyResolver.resolve(market);
		return cache.findQuote(market, ticker).or(() -> findAndCacheQuote(strategy, market, ticker));
	}

	private List<AssetCandidate> findAndCache(AssetSearchStrategy strategy, AssetMarket market, AssetType type, String query) {
		List<AssetCandidate> candidates = strategy.findCandidates(type, query).stream()
				.filter(candidate -> candidate.market() == market && candidate.assetType() == type).toList();
		cache.storeCandidates(market, type, query, candidates);
		return candidates;
	}
	private java.util.Optional<AssetQuote> findAndCacheQuote(AssetSearchStrategy strategy, AssetMarket market, String ticker) {
		return strategy.findQuote(ticker).map(quote -> { cache.storeQuote(market, ticker, quote); return quote; });
	}
	private AssetSearchResponse toResponse(UUID ownerId, AssetCandidate candidate, AssetQuote quote) {
		SelectedAsset asset = new SelectedAsset(candidate.ticker(), candidate.name(), candidate.market(), candidate.assetType(), quote.currency());
		return new AssetSearchResponse(selectionCache.store(ownerId, asset), asset.ticker(), asset.name(), asset.market(), asset.assetType(),
				asset.currency(), quote.price());
	}
	private AssetMarket market(String value) { return parse(value, AssetMarket.class, "market"); }
	private AssetType type(String value) { return parse(value, AssetType.class, "assetType"); }
	private String query(String value) {
		String normalized = value == null ? "" : value.trim();
		if (normalized.length() < 2) throw invalid("query", "Must contain at least 2 characters");
		return normalized.toUpperCase(Locale.ROOT);
	}
	private <T extends Enum<T>> T parse(String value, Class<T> type, String field) {
		try { return Enum.valueOf(type, value == null ? "" : value.trim().toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException exception) { throw invalid(field, "Invalid value"); }
	}
	private InvalidAssetSearchDataException invalid(String field, String message) {
		return new InvalidAssetSearchDataException(List.of(new FieldErrorResponse(field, message)));
	}
}
