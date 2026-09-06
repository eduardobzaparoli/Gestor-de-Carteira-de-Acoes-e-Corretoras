package com.bominvestidor.spring.service.asset;

import com.bominvestidor.spring.domain.asset.AssetMarket;

public final class AssetIdentityRules {
	private AssetIdentityRules() { }
	public static boolean hasCompatibleCurrency(AssetMarket market, String currency) {
		return market == AssetMarket.BR ? "BRL".equalsIgnoreCase(currency) : "USD".equalsIgnoreCase(currency);
	}
}
