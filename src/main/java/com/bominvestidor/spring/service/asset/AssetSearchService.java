package com.bominvestidor.spring.service.asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.dto.asset.AssetSearchResponse;
import com.bominvestidor.spring.dto.error.FieldErrorResponse;
import com.bominvestidor.spring.exception.InvalidAssetSearchDataException;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategy;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

@Service
public class AssetSearchService {
	private static final int MAX_RESULTS = 5;
	private final PortfolioService portfolioService;
	private final AssetSearchStrategyResolver strategyResolver;
	private final AssetSearchCache cache;

	public AssetSearchService(PortfolioService portfolioService, AssetSearchStrategyResolver strategyResolver,
			AssetSearchCache cache) {
		this.portfolioService = portfolioService;
		this.strategyResolver = strategyResolver;
		this.cache = cache;
	}

	public List<AssetSearchResponse> search(UUID ownerId, UUID portfolioId, String marketValue, String typeValue,
			String queryValue) {
		portfolioService.requireOwnedPortfolio(ownerId, portfolioId);
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
					.ifPresent(quote -> results.add(toResponse(candidate, quote)));
		}
		return List.copyOf(results);
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
	private AssetSearchResponse toResponse(AssetCandidate candidate, AssetQuote quote) {
		return new AssetSearchResponse(candidate.ticker(), candidate.name(), candidate.market(), candidate.assetType(),
				quote.currency(), quote.price());
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
