package com.bominvestidor.spring.domain.asset;

public record SelectedAsset(String ticker, String name, AssetMarket market, AssetType assetType, String currency) { }
