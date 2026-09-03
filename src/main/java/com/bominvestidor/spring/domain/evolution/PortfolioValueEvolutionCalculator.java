package com.bominvestidor.spring.domain.evolution;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.valuation.HistoricalPositionAccumulator;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

/** Pure, incremental replay of the custody log for the value-evolution graph. */
public final class PortfolioValueEvolutionCalculator {
	private static final MathContext PRECISION = MathContext.DECIMAL128;

	public List<PortfolioValueEvolutionPoint> calculate(List<PortfolioTransactionEntity> transactions, List<LocalDate> dates,
			Map<HistoricalAssetKey, HistoricalAssetPriceSeries> series, Map<UUID, ExchangeRate> purchaseRates,
			Map<LocalDate, ExchangeRate> pointRates) {
		List<PortfolioTransactionEntity> effective = transactions.stream()
				.filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE)
				.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate).thenComparing(PortfolioTransactionEntity::getCreatedAt)).toList();
		Map<HistoricalAssetKey, HistoricalPositionAccumulator> positions = new LinkedHashMap<>();
		List<PortfolioValueEvolutionPoint> result = new ArrayList<>();
		int next = 0;
		for (LocalDate date : dates) {
			while (next < effective.size() && !effective.get(next).getTransactionDate().isAfter(date)) {
				PortfolioTransactionEntity transaction = effective.get(next++);
				HistoricalAssetKey key = new HistoricalAssetKey(transaction.getMarket(), transaction.getTicker(), transaction.getCurrency());
				positions.computeIfAbsent(key, ignored -> new HistoricalPositionAccumulator()).apply(transaction, purchaseRates.get(transaction.getId()));
			}
			BigDecimal invested = BigDecimal.ZERO;
			BigDecimal market = BigDecimal.ZERO;
			for (var entry : positions.entrySet()) {
				HistoricalPositionAccumulator position = entry.getValue();
				if (position.quantity().signum() == 0) continue;
				HistoricalAssetPrice price = series.get(entry.getKey()).latestAt(date);
				if (price == null) throw new IllegalArgumentException("Missing historical price");
				BigDecimal value = position.quantity().multiply(price.close(), PRECISION);
				if (!"BRL".equals(entry.getKey().currency())) {
					ExchangeRate rate = pointRates.get(date);
					if (rate == null) throw new IllegalArgumentException("Missing historical exchange rate");
					value = value.multiply(rate.rate(), PRECISION);
				}
				invested = invested.add(position.costInBrl(), PRECISION);
				market = market.add(value, PRECISION);
			}
			if (positions.values().stream().anyMatch(position -> position.quantity().signum() > 0)) result.add(new PortfolioValueEvolutionPoint(date, invested, market));
		}
		return List.copyOf(result);
	}
}
