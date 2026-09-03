package com.bominvestidor.spring.domain.evolution;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public record HistoricalAssetPriceSeries(HistoricalAssetKey asset, NavigableMap<LocalDate, HistoricalAssetPrice> prices) {
	public HistoricalAssetPriceSeries {
		if (asset == null || prices == null || prices.isEmpty()) throw new IllegalArgumentException("Historical series is invalid");
		prices = new TreeMap<>(prices);
	}
	public HistoricalAssetPrice latestAt(LocalDate date) {
		var entry = prices.floorEntry(date);
		return entry == null ? null : entry.getValue();
	}
	public List<HistoricalAssetPrice> orderedPrices() {
		return prices.values().stream().sorted(Comparator.comparing(HistoricalAssetPrice::date)).toList();
	}
}
