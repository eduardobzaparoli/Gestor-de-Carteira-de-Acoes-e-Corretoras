package com.bominvestidor.spring.integration.historicalprice;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.asset.AssetMarket;

@Component
public class HistoricalAssetPriceStrategyResolver {
	private final Map<AssetMarket, HistoricalAssetPriceStrategy> strategies = new EnumMap<>(AssetMarket.class);
	public HistoricalAssetPriceStrategyResolver(List<HistoricalAssetPriceStrategy> strategies) {
		strategies.forEach(strategy -> this.strategies.put(strategy.market(), strategy));
	}
	public HistoricalAssetPriceStrategy resolve(AssetMarket market) { return strategies.get(market); }
}
