package com.bominvestidor.spring.integration.income;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.asset.AssetMarket;

@Component
public class IncomeEventProviderStrategyResolver {
	private final Map<AssetMarket, IncomeEventProviderStrategy> strategies = new EnumMap<>(AssetMarket.class);

	public IncomeEventProviderStrategyResolver(List<IncomeEventProviderStrategy> strategies) {
		strategies.forEach(strategy -> this.strategies.put(strategy.market(), strategy));
	}

	public IncomeEventProviderStrategy resolve(AssetMarket market) { return strategies.get(market); }
}
