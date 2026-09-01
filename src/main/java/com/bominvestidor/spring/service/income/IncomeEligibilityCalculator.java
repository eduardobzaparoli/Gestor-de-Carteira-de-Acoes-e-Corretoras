package com.bominvestidor.spring.service.income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

public final class IncomeEligibilityCalculator {
	private IncomeEligibilityCalculator() { }

	public static BigDecimal quantityAt(List<PortfolioTransactionEntity> transactions, String ticker, AssetMarket market,
			LocalDate eligibilityDate, boolean inclusive) {
		return transactions.stream()
			.filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE)
			.filter(item -> item.getTicker().equalsIgnoreCase(ticker) && item.getMarket() == market)
			.filter(item -> inclusive ? !item.getTransactionDate().isAfter(eligibilityDate)
					: item.getTransactionDate().isBefore(eligibilityDate))
			.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate)
					.thenComparing(PortfolioTransactionEntity::getCreatedAt))
			.map(item -> item.getType() == TransactionType.BUY ? item.getQuantity() : item.getQuantity().negate())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
