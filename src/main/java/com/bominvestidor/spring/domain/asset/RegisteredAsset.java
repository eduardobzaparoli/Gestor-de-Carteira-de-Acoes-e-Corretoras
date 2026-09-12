package com.bominvestidor.spring.domain.asset;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegisteredAsset(UUID id, UUID ownerId, String ticker, String name, AssetMarket market,
		AssetType assetType, String currency, BigDecimal lastQuote, Instant quotedAt, Instant createdAt,
		Instant updatedAt) {
}
