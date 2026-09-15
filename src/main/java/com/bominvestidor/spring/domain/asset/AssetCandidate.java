package com.bominvestidor.spring.domain.asset;

public record AssetCandidate(String ticker, String name, AssetMarket market, AssetType assetType, String currency) {
}
