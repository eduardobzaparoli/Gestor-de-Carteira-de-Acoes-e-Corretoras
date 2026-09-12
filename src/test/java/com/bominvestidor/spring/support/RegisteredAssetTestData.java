package com.bominvestidor.spring.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.entity.asset.RegisteredAssetEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.asset.RegisteredAssetRepository;

public final class RegisteredAssetTestData {
	private RegisteredAssetTestData() {
	}

	public static UUID registeredAsset(RegisteredAssetRepository repository, UserEntity owner, String ticker,
			String name, AssetMarket market, AssetType type, String currency) {
		String normalizedTicker = ticker.trim().toUpperCase(Locale.ROOT);
		return repository.findByOwner_IdAndMarketAndTicker(owner.getId(), market, normalizedTicker)
				.map(RegisteredAssetEntity::getId)
				.orElseGet(() -> {
					Instant now = Instant.now();
					return repository.saveAndFlush(new RegisteredAssetEntity(UUID.randomUUID(), owner, normalizedTicker,
							name, market, type, currency, new BigDecimal("35.10"), now, now, now)).getId();
				});
	}
}
