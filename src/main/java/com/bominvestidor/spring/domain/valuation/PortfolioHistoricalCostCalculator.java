package com.bominvestidor.spring.domain.valuation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

public final class PortfolioHistoricalCostCalculator {
	private static final MathContext PRECISION = MathContext.DECIMAL128;

	public PortfolioHistoricalCost calculate(List<PortfolioTransactionEntity> transactions, Map<UUID, ExchangeRate> historicalRates) {
		Map<PositionKey, Accumulator> positions = new LinkedHashMap<>();
		Map<String, ExchangeRate> ratesUsed = new LinkedHashMap<>();
		transactions.stream().filter(transaction -> transaction.getStatus() == TransactionStatus.EFFECTIVE)
				.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate)
						.thenComparing(PortfolioTransactionEntity::getCreatedAt))
				.forEach(transaction -> positions.computeIfAbsent(new PositionKey(transaction), ignored -> new Accumulator())
						.apply(transaction, historicalRates.get(transaction.getId()), ratesUsed));
		BigDecimal investedValue = positions.values().stream().map(Accumulator::costInBrl)
				.reduce(BigDecimal.ZERO, (left, right) -> left.add(right, PRECISION));
		return new PortfolioHistoricalCost(investedValue, ratesUsed.values().stream()
				.sorted(Comparator.comparing(ExchangeRate::referenceDate).thenComparing(ExchangeRate::sourceCurrency)).toList());
	}

	private record PositionKey(String ticker, com.bominvestidor.spring.domain.asset.AssetMarket market) {
		private PositionKey(PortfolioTransactionEntity transaction) { this(transaction.getTicker(), transaction.getMarket()); }
	}

	private static final class Accumulator {
		private BigDecimal quantity = BigDecimal.ZERO;
		private BigDecimal costInBrl = BigDecimal.ZERO;

		private void apply(PortfolioTransactionEntity transaction, ExchangeRate rate, Map<String, ExchangeRate> ratesUsed) {
			if (transaction.getType() == TransactionType.BUY) {
				quantity = quantity.add(transaction.getQuantity(), PRECISION);
				BigDecimal cost = transaction.getQuantity().multiply(transaction.getUnitPrice(), PRECISION).add(transaction.getCosts(), PRECISION);
				if (!"BRL".equalsIgnoreCase(transaction.getCurrency())) {
					if (rate == null) throw new IllegalArgumentException("Missing historical exchange rate");
					cost = cost.multiply(rate.rate(), PRECISION);
					ratesUsed.put(rate.sourceCurrency() + ":" + rate.referenceDate(), rate);
				}
				costInBrl = costInBrl.add(cost, PRECISION);
				return;
			}
			BigDecimal averageCost = quantity.signum() == 0 ? BigDecimal.ZERO : costInBrl.divide(quantity, PRECISION);
			quantity = quantity.subtract(transaction.getQuantity(), PRECISION);
			costInBrl = costInBrl.subtract(transaction.getQuantity().multiply(averageCost, PRECISION), PRECISION);
			if (quantity.signum() == 0) { quantity = BigDecimal.ZERO; costInBrl = BigDecimal.ZERO; }
		}

		private BigDecimal costInBrl() { return costInBrl; }
	}
}
