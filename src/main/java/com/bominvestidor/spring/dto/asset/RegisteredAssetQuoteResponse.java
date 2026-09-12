package com.bominvestidor.spring.dto.asset;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;

public record RegisteredAssetQuoteResponse(UUID assetId, String ticker, AssetMarket market, String currency,
		BigDecimal price, Instant quotedAt) {
}
