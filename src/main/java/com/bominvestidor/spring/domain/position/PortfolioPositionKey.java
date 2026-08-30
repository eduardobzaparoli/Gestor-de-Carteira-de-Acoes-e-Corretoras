package com.bominvestidor.spring.domain.position;

import java.util.Locale;

import com.bominvestidor.spring.domain.asset.AssetMarket;

public record PortfolioPositionKey(String ticker, AssetMarket market) {
	public PortfolioPositionKey {
		ticker = ticker.trim().toUpperCase(Locale.ROOT);
	}
}
