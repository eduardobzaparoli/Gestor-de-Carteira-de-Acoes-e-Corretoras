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
		Map<PositionKey, HistoricalPositionAccumulator> positions = new LinkedHashMap<>();
		Map<String, ExchangeRate> ratesUsed = new LinkedHashMap<>();
		transactions.stream().filter(transaction -> transaction.getStatus() == TransactionStatus.EFFECTIVE)
				.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate)
						.thenComparing(PortfolioTransactionEntity::getCreatedAt))
				.forEach(transaction -> {
					ExchangeRate rate = historicalRates.get(transaction.getId());
					positions.computeIfAbsent(new PositionKey(transaction), ignored -> new HistoricalPositionAccumulator())
							.apply(transaction, rate);
					if (transaction.getType() == TransactionType.BUY && !"BRL".equalsIgnoreCase(transaction.getCurrency()) && rate != null) {
						ratesUsed.put(rate.sourceCurrency() + ":" + rate.referenceDate(), rate);
					}
				});
		BigDecimal investedValue = positions.values().stream().map(HistoricalPositionAccumulator::costInBrl)
				.reduce(BigDecimal.ZERO, (left, right) -> left.add(right, PRECISION));
		return new PortfolioHistoricalCost(investedValue, ratesUsed.values().stream()
				.sorted(Comparator.comparing(ExchangeRate::referenceDate).thenComparing(ExchangeRate::sourceCurrency)).toList());
	}

	private record PositionKey(String ticker, com.bominvestidor.spring.domain.asset.AssetMarket market) {
		private PositionKey(PortfolioTransactionEntity transaction) { this(transaction.getTicker(), transaction.getMarket()); }
	}
}
