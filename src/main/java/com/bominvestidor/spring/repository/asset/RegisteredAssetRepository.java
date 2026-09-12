package com.bominvestidor.spring.repository.asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.entity.asset.RegisteredAssetEntity;

public interface RegisteredAssetRepository extends JpaRepository<RegisteredAssetEntity, UUID> {
	Optional<RegisteredAssetEntity> findByIdAndOwner_Id(UUID id, UUID ownerId);
	Optional<RegisteredAssetEntity> findByOwner_IdAndMarketAndTicker(UUID ownerId, AssetMarket market, String ticker);
	boolean existsByOwner_IdAndMarketAndTicker(UUID ownerId, AssetMarket market, String ticker);
	List<RegisteredAssetEntity> findAllByOwner_IdOrderByMarketAscTickerAscIdAsc(UUID ownerId);
	List<RegisteredAssetEntity> findAllByOwner_IdAndMarketOrderByTickerAscIdAsc(UUID ownerId, AssetMarket market);
}
