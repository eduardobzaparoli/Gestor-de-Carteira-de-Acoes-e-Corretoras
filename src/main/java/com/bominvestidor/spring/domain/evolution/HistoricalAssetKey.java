package com.bominvestidor.spring.domain.evolution;

import java.util.Locale;

import com.bominvestidor.spring.domain.asset.AssetMarket;

public record HistoricalAssetKey(AssetMarket market, String ticker, String currency) {
	public HistoricalAssetKey {
		if (market == null || ticker == null || ticker.isBlank() || currency == null || currency.isBlank()) {
			throw new IllegalArgumentException("Historical asset key is invalid");
		}
		ticker = ticker.trim().toUpperCase(Locale.ROOT);
		currency = currency.trim().toUpperCase(Locale.ROOT);
	}
}
