package com.bominvestidor.spring.domain.valuation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

class PortfolioHistoricalCostCalculatorTests {
	private final PortfolioHistoricalCostCalculator calculator = new PortfolioHistoricalCostCalculator();

	@Test
	void convertsUsdPurchaseAndReducesItsHistoricalAverageCostOnSale() {
		PortfolioTransactionEntity buy = transaction(TransactionType.BUY, TransactionStatus.EFFECTIVE, "USD", "10", "10", "2", 1);
		PortfolioTransactionEntity sale = transaction(TransactionType.SELL, TransactionStatus.EFFECTIVE, "USD", "4", "30", "0", 2);
		ExchangeRate rate = new ExchangeRate("USD", "BRL", new BigDecimal("5"), LocalDate.of(2026, 8, 28));

		PortfolioHistoricalCost result = calculator.calculate(List.of(buy, sale), Map.of(buy.getId(), rate));

		assertEquals(0, new BigDecimal("306").compareTo(result.investedValue()));
		assertEquals(List.of(rate), result.exchangeRates());
	}

	@Test
	void ignoresPendingTransactions() {
		PortfolioTransactionEntity buy = transaction(TransactionType.BUY, TransactionStatus.EFFECTIVE, "BRL", "2", "10", "1", 1);
		PortfolioTransactionEntity pending = transaction(TransactionType.BUY, TransactionStatus.PENDING, "USD", "5", "10", "0", 2);

		PortfolioHistoricalCost result = calculator.calculate(List.of(buy, pending), Map.of());

		assertEquals(0, new BigDecimal("21").compareTo(result.investedValue()));
		assertEquals(List.of(), result.exchangeRates());
	}

	private PortfolioTransactionEntity transaction(TransactionType type, TransactionStatus status, String currency, String quantity,
			String price, String costs, int order) {
		Instant createdAt = Instant.parse("2026-08-28T12:00:00Z").plusSeconds(order);
		return new PortfolioTransactionEntity(UUID.randomUUID(), null, "MSFT", "Microsoft", AssetMarket.US, AssetType.STOCK, currency,
				type, status, LocalDate.of(2026, 8, 28), new BigDecimal(quantity), new BigDecimal(price), new BigDecimal(costs), createdAt, createdAt);
	}
}
