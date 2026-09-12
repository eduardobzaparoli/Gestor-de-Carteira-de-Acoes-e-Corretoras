package com.bominvestidor.spring.mapper.asset;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.asset.RegisteredAsset;
import com.bominvestidor.spring.dto.asset.RegisteredAssetResponse;
import com.bominvestidor.spring.entity.asset.RegisteredAssetEntity;
import com.bominvestidor.spring.entity.user.UserEntity;

@Component
public class RegisteredAssetMapper {
	public RegisteredAsset toDomain(RegisteredAssetEntity entity) {
		return new RegisteredAsset(entity.getId(), entity.getOwner().getId(), entity.getTicker(), entity.getName(),
				entity.getMarket(), entity.getAssetType(), entity.getCurrency(), entity.getLastQuote(), entity.getQuotedAt(),
				entity.getCreatedAt(), entity.getUpdatedAt());
	}

	public RegisteredAssetEntity toEntity(RegisteredAsset asset, UserEntity owner) {
		return new RegisteredAssetEntity(asset.id(), owner, asset.ticker(), asset.name(), asset.market(),
				asset.assetType(), asset.currency(), asset.lastQuote(), asset.quotedAt(), asset.createdAt(), asset.updatedAt());
	}

	public RegisteredAssetResponse toResponse(RegisteredAssetEntity entity) {
		return toResponse(toDomain(entity));
	}

	public RegisteredAssetResponse toResponse(RegisteredAsset asset) {
		return new RegisteredAssetResponse(asset.id(), asset.ticker(), asset.name(), asset.market(), asset.assetType(),
				asset.currency(), asset.lastQuote(), asset.quotedAt(), asset.createdAt(), asset.updatedAt());
	}
}
