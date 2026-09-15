package com.bominvestidor.spring.dto.asset;

import java.math.BigDecimal;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

public record AssetSearchResponse(UUID selectionId, String ticker, String name, AssetMarket market, AssetType assetType,
		String currency, BigDecimal price) {
}
