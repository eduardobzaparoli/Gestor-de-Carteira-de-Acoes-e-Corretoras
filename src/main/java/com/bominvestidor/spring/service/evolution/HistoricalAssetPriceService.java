package com.bominvestidor.spring.service.evolution;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;
import com.bominvestidor.spring.exception.HistoricalPriceUnavailableException;
import com.bominvestidor.spring.integration.historicalprice.HistoricalAssetPriceStrategy;
import com.bominvestidor.spring.integration.historicalprice.HistoricalAssetPriceStrategyResolver;

@Service
public class HistoricalAssetPriceService {
	private final HistoricalAssetPriceStrategyResolver resolver;
	private final HistoricalAssetPriceCache cache;
	public HistoricalAssetPriceService(HistoricalAssetPriceStrategyResolver resolver, HistoricalAssetPriceCache cache) { this.resolver = resolver; this.cache = cache; }
	public HistoricalAssetPriceSeries find(AssetMarket market, String ticker, String currency, LocalDate from, LocalDate to) {
		return cache.find(market, ticker, from, to).orElseGet(() -> {
			HistoricalAssetPriceStrategy strategy = resolver.resolve(market);
			if (strategy == null) throw new HistoricalPriceUnavailableException();
			HistoricalAssetPriceSeries value = strategy.findSeries(ticker, currency, from, to);
			if (value == null || value.prices().isEmpty()) throw new HistoricalPriceUnavailableException();
			cache.store(market, ticker, from, to, value);
			return value;
		});
	}
}
