package com.bominvestidor.spring.integration.income;

import java.util.List;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.income.IncomeProviderEvent;

public interface IncomeEventProviderStrategy {
	AssetMarket market();
	List<IncomeProviderEvent> findEvents(String ticker);
}
