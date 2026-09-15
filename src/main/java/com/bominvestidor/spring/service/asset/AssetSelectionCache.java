package com.bominvestidor.spring.service.asset;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.SelectedAsset;

@Component
public class AssetSelectionCache {
	private final ConcurrentHashMap<UUID, Entry> entries = new ConcurrentHashMap<>();
	private final Clock clock;
	private final BrokerageIntegrationProperties properties;

	public AssetSelectionCache(Clock clock, BrokerageIntegrationProperties properties) {
		this.clock = clock;
		this.properties = properties;
	}

	public UUID store(UUID ownerId, SelectedAsset asset) {
		UUID id = UUID.randomUUID();
		entries.put(id, new Entry(ownerId, asset, clock.instant().plus(properties.getAssetSearchCacheTtl())));
		return id;
	}

	public Optional<SelectedAsset> find(UUID id, UUID ownerId) {
		Entry entry = entries.get(id);
		if (entry == null || !entry.ownerId().equals(ownerId)
				|| !entry.expiresAt().isAfter(clock.instant())) {
			entries.remove(id);
			return Optional.empty();
		}
		return Optional.of(entry.asset());
	}

	public void remove(UUID id) { entries.remove(id); }
	private record Entry(UUID ownerId, SelectedAsset asset, Instant expiresAt) { }
}
