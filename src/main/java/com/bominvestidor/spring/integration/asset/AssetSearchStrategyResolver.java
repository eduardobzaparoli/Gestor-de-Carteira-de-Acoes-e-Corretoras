package com.bominvestidor.spring.integration.asset;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.asset.AssetMarket;

@Component
public class AssetSearchStrategyResolver {

	private final Map<AssetMarket, AssetSearchStrategy> strategies = new EnumMap<>(AssetMarket.class);

	public AssetSearchStrategyResolver(List<AssetSearchStrategy> strategies) {
		strategies.forEach(strategy -> this.strategies.put(strategy.market(), strategy));
	}

	public AssetSearchStrategy resolve(AssetMarket market) {
		return strategies.get(market);
	}
}
