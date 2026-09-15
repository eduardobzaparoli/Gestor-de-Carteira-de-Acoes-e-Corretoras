package com.bominvestidor.spring.service.asset;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.exception.AssetQuoteUnavailableException;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;

@Service
public class FreshAssetQuoteService {
	private final AssetSearchStrategyResolver strategyResolver;
	private final AssetSearchCache cache;

	public FreshAssetQuoteService(AssetSearchStrategyResolver strategyResolver, AssetSearchCache cache) {
		this.strategyResolver = strategyResolver;
		this.cache = cache;
	}

	public AssetQuote fetch(AssetMarket market, String ticker) {
		AssetQuote quote = strategyResolver.resolve(market).findQuote(ticker)
				.filter(value -> value.price() != null && value.price().signum() > 0)
				.filter(value -> AssetIdentityRules.hasCompatibleCurrency(market, value.currency()))
				.orElseThrow(AssetQuoteUnavailableException::new);
		cache.storeQuote(market, ticker, quote);
		return quote;
	}
}
