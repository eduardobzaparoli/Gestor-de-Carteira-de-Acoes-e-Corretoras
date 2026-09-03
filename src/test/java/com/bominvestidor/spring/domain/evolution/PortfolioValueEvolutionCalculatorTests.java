package com.bominvestidor.spring.domain.evolution;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

class PortfolioValueEvolutionCalculatorTests {
	private final PortfolioValueEvolutionCalculator calculator = new PortfolioValueEvolutionCalculator();
	private final HistoricalAssetKey petr = new HistoricalAssetKey(AssetMarket.BR, "PETR4", "BRL");

	@Test
	void reducesInvestedValueByAverageCostInsteadOfSaleProceeds() {
		LocalDate first = LocalDate.of(2026, 8, 3); LocalDate second = first.plusDays(1);
		var values = calculator.calculate(List.of(transaction(TransactionType.BUY, first, "10", "10", "2"), transaction(TransactionType.SELL, second, "4", "50", "0")),
				List.of(first, second), Map.of(petr, series(first, "12", second, "13")), Map.of(), Map.of());
		assertEquals(0, new BigDecimal("102").compareTo(values.get(0).investedValue()));
		assertEquals(0, new BigDecimal("61.2").compareTo(values.get(1).investedValue()));
		assertEquals(0, new BigDecimal("78").compareTo(values.get(1).marketValue()));
	}

	@Test
	void liquidationResetsCostAndRepurchaseStartsNewBasis() {
		LocalDate first = LocalDate.of(2026, 8, 3); LocalDate second = first.plusDays(1); LocalDate third = second.plusDays(1);
		var values = calculator.calculate(List.of(transaction(TransactionType.BUY, first, "10", "10", "0"), transaction(TransactionType.SELL, second, "10", "99", "0"), transaction(TransactionType.BUY, third, "2", "7", "0")),
				List.of(first, second, third), Map.of(petr, series(first, "10", second, "11", third, "8")), Map.of(), Map.of());
		assertEquals(2, values.size());
		assertEquals(0, new BigDecimal("14").compareTo(values.get(1).investedValue()));
	}

	private HistoricalAssetPriceSeries series(LocalDate first, String firstClose, LocalDate second, String secondClose) {
		TreeMap<LocalDate, HistoricalAssetPrice> prices = new TreeMap<>(); prices.put(first, new HistoricalAssetPrice(first, new BigDecimal(firstClose))); prices.put(second, new HistoricalAssetPrice(second, new BigDecimal(secondClose)));
		return new HistoricalAssetPriceSeries(petr, prices);
	}
	private HistoricalAssetPriceSeries series(LocalDate first, String firstClose, LocalDate second, String secondClose, LocalDate third, String thirdClose) {
		TreeMap<LocalDate, HistoricalAssetPrice> prices = new TreeMap<>(); prices.put(first, new HistoricalAssetPrice(first, new BigDecimal(firstClose))); prices.put(second, new HistoricalAssetPrice(second, new BigDecimal(secondClose))); prices.put(third, new HistoricalAssetPrice(third, new BigDecimal(thirdClose)));
		return new HistoricalAssetPriceSeries(petr, prices);
	}
	private PortfolioTransactionEntity transaction(TransactionType type, LocalDate date, String quantity, String price, String costs) {
		return new PortfolioTransactionEntity(UUID.randomUUID(), null, "PETR4", "Petrobras", AssetMarket.BR, AssetType.STOCK, "BRL", type, TransactionStatus.EFFECTIVE,
				date, new BigDecimal(quantity), new BigDecimal(price), new BigDecimal(costs), Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"));
	}
}
