package com.bominvestidor.spring.domain.position;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

public final class PortfolioPositionCalculator {
	private static final MathContext PRECISION = MathContext.DECIMAL128;

	public List<PortfolioPosition> calculate(List<PortfolioTransactionEntity> transactions) {
		Map<PortfolioPositionKey, Accumulator> positions = new LinkedHashMap<>();
		transactions.stream()
			.filter(transaction -> transaction.getStatus() == TransactionStatus.EFFECTIVE)
			.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate)
				.thenComparing(PortfolioTransactionEntity::getCreatedAt))
			.forEach(transaction -> positions.computeIfAbsent(new PortfolioPositionKey(transaction.getTicker(), transaction.getMarket()),
				key -> new Accumulator(transaction)).apply(transaction));
		return positions.values().stream().filter(Accumulator::isOpen).map(Accumulator::toPosition)
			.sorted(Comparator.comparing(PortfolioPosition::market).thenComparing(PortfolioPosition::ticker)).toList();
	}

	private static final class Accumulator {
		private BigDecimal quantity = BigDecimal.ZERO;
		private BigDecimal custodyCost = BigDecimal.ZERO;
		private String ticker;
		private String assetName;
		private com.bominvestidor.spring.domain.asset.AssetMarket market;
		private com.bominvestidor.spring.domain.asset.AssetType assetType;
		private String currency;

		private Accumulator(PortfolioTransactionEntity transaction) { updateSnapshot(transaction); }

		private void apply(PortfolioTransactionEntity transaction) {
			updateSnapshot(transaction);
			if (transaction.getType() == TransactionType.BUY) {
				quantity = quantity.add(transaction.getQuantity());
				custodyCost = custodyCost.add(transaction.getQuantity().multiply(transaction.getUnitPrice(), PRECISION)
					.add(transaction.getCosts()));
				return;
			}
			BigDecimal averagePrice = averagePrice();
			quantity = quantity.subtract(transaction.getQuantity());
			custodyCost = custodyCost.subtract(transaction.getQuantity().multiply(averagePrice, PRECISION));
			if (quantity.compareTo(BigDecimal.ZERO) == 0) {
				quantity = BigDecimal.ZERO;
				custodyCost = BigDecimal.ZERO;
			}
		}

		private void updateSnapshot(PortfolioTransactionEntity transaction) {
			ticker = transaction.getTicker(); assetName = transaction.getAssetName(); market = transaction.getMarket();
			assetType = transaction.getAssetType(); currency = transaction.getCurrency();
		}
		private BigDecimal averagePrice() { return quantity.signum() == 0 ? BigDecimal.ZERO : custodyCost.divide(quantity, PRECISION); }
		private boolean isOpen() { return quantity.signum() > 0; }
		private PortfolioPosition toPosition() { return new PortfolioPosition(ticker, assetName, market, assetType, currency, quantity, averagePrice(), custodyCost); }
	}
}
