package com.bominvestidor.spring.dto.asset;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

public record RegisteredAssetResponse(UUID id, String ticker, String name, AssetMarket market, AssetType assetType,
		String currency, BigDecimal lastQuote, Instant quotedAt, Instant createdAt, Instant updatedAt) {
}
