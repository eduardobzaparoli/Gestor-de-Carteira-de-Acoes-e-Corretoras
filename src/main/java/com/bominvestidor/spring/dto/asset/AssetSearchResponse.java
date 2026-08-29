package com.bominvestidor.spring.dto.asset;

import java.math.BigDecimal;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

public record AssetSearchResponse(String ticker, String name, AssetMarket market, AssetType assetType,
		String currency, BigDecimal price) {
}
