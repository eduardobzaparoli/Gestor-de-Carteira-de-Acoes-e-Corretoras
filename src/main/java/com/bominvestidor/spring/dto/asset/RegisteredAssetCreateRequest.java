package com.bominvestidor.spring.dto.asset;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RegisteredAssetCreateRequest(@NotNull UUID assetSelectionId) {
}
