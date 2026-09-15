package com.bominvestidor.spring.domain.valuation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;

class PortfolioValuationCalculatorTests {
	private final PortfolioValuationCalculator calculator = new PortfolioValuationCalculator();

	@Test
	void calculatesPositionsAndSeparateCurrencySummaries() {
		PortfolioPositionResponse petr = position("PETR4", AssetMarket.BR, "BRL", "10", "100");
		PortfolioPositionResponse vale = position("VALE3", AssetMarket.BR, "BRL", "5", "50");
		PortfolioPositionResponse msft = position("MSFT", AssetMarket.US, "USD", "2", "200");
		var result = calculator.calculate(List.of(petr, vale, msft), Map.of(
				key(petr), quote("PETR4", "BRL", "12"), key(vale), quote("VALE3", "BRL", "8"), key(msft), quote("MSFT", "USD", "120")),
				Map.of("USD", rate("5")));

		assertEquals(3, result.positions().size());
		assertEquals(0, new BigDecimal("120").compareTo(result.positions().get(0).marketValue()));
		assertEquals(0, new BigDecimal("20").compareTo(result.positions().get(0).unrealizedGain()));
		assertEquals(0, new BigDecimal("8.82").compareTo(result.positions().get(0).allocationPercentage()));
		assertEquals(0, result.positions().stream().map(PortfolioValuationPosition::allocationPercentage)
				.reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(new BigDecimal("100.00")));
		assertEquals(2, result.currencySummaries().size());
		assertEquals("BRL", result.currencySummaries().get(0).currency());
		assertEquals(0, new BigDecimal("160").compareTo(result.currencySummaries().get(0).marketValue()));
		assertEquals("USD", result.currencySummaries().get(1).currency());
		assertEquals(0, new BigDecimal("240").compareTo(result.currencySummaries().get(1).marketValue()));
		assertEquals("BRL", result.consolidatedSummary().baseCurrency());
		assertEquals(0, new BigDecimal("1360").compareTo(result.consolidatedSummary().marketValue()));
		assertEquals(1, result.consolidatedSummary().exchangeRates().size());
	}

	@Test
	void returnsEmptyValuationForNoOpenPositions() {
		var result = calculator.calculate(List.of(), Map.of(), Map.of());
		assertEquals(List.of(), result.positions());
		assertEquals(List.of(), result.currencySummaries());
		assertEquals(null, result.consolidatedSummary());
	}

	@Test
	void consolidatesABrlOnlyPortfolioWithoutExchangeRates() {
		PortfolioPositionResponse petr = position("PETR4", AssetMarket.BR, "BRL", "10", "100");
		var result = calculator.calculate(List.of(petr), Map.of(key(petr), quote("PETR4", "BRL", "12")), Map.of());

		assertEquals(0, new BigDecimal("120").compareTo(result.consolidatedSummary().marketValue()));
		assertEquals(List.of(), result.consolidatedSummary().exchangeRates());
	}

	@Test
	void convertsAnUsdOnlyPortfolioToBrl() {
		PortfolioPositionResponse msft = position("MSFT", AssetMarket.US, "USD", "2", "200");
		var result = calculator.calculate(List.of(msft), Map.of(key(msft), quote("MSFT", "USD", "120")),
				Map.of("USD", rate("5")));

		assertEquals(0, new BigDecimal("1200").compareTo(result.consolidatedSummary().marketValue()));
		assertEquals(1, result.consolidatedSummary().exchangeRates().size());
		assertEquals(0, new BigDecimal("100.00").compareTo(result.positions().get(0).allocationPercentage()));
	}

	@Test
	void closesRepeatingAllocationsAtOneHundredIndependentlyOfInputOrder() {
		PortfolioPositionResponse aaa = position("AAA3", AssetMarket.BR, "BRL", "1", "1");
		PortfolioPositionResponse bbb = position("BBB3", AssetMarket.BR, "BRL", "1", "1");
		PortfolioPositionResponse ccc = position("CCC3", AssetMarket.BR, "BRL", "1", "1");
		Map<PortfolioValuationCalculator.PositionKey, AssetQuote> quotes = Map.of(key(aaa), quote("AAA3", "BRL", "1"),
				key(bbb), quote("BBB3", "BRL", "1"), key(ccc), quote("CCC3", "BRL", "1"));
		var first = calculator.calculate(List.of(aaa, bbb, ccc), quotes, Map.of());
		var reversed = calculator.calculate(List.of(ccc, bbb, aaa), quotes, Map.of());
		assertEquals(new BigDecimal("100.00"), first.positions().stream().map(PortfolioValuationPosition::allocationPercentage)
				.reduce(BigDecimal.ZERO, BigDecimal::add));
		Map<String, BigDecimal> firstByTicker = first.positions().stream().collect(java.util.stream.Collectors.toMap(
				PortfolioValuationPosition::ticker, PortfolioValuationPosition::allocationPercentage));
		reversed.positions().forEach(position -> assertEquals(firstByTicker.get(position.ticker()), position.allocationPercentage()));
	}

	private PortfolioPositionResponse position(String ticker, AssetMarket market, String currency, String quantity, String cost) {
		return new PortfolioPositionResponse(ticker, ticker, market, AssetType.STOCK, currency, new BigDecimal(quantity),
				new BigDecimal(cost).divide(new BigDecimal(quantity)), new BigDecimal(cost));
	}
	private PortfolioValuationCalculator.PositionKey key(PortfolioPositionResponse position) { return new PortfolioValuationCalculator.PositionKey(position.market(), position.ticker()); }
	private AssetQuote quote(String ticker, String currency, String price) { return new AssetQuote(ticker, currency, new BigDecimal(price)); }
	private ExchangeRate rate(String value) { return new ExchangeRate("USD", "BRL", new BigDecimal(value), java.time.LocalDate.of(2026, 8, 28)); }
}
