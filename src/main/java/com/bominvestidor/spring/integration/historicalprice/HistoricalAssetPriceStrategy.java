package com.bominvestidor.spring.integration.historicalprice;

import java.time.LocalDate;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;

public interface HistoricalAssetPriceStrategy {
	AssetMarket market();
	HistoricalAssetPriceSeries findSeries(String ticker, String currency, LocalDate startDate, LocalDate endDate);
}
