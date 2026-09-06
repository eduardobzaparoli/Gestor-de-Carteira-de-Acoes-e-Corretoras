package com.bominvestidor.spring.service.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.exception.PortfolioTransactionConflictException;

@Service
public class PortfolioTransactionBalanceService {

	public void validateSale(List<PortfolioTransactionEntity> transactions, PortfolioTransactionEntity candidate) {
		if (candidate.getType() != TransactionType.SELL) return;
		validateChronologicalBalance(transactions, candidate);
		if (availableQuantity(transactions, candidate).compareTo(candidate.getQuantity()) < 0) throw insufficientQuantity();
	}

	private void validateChronologicalBalance(List<PortfolioTransactionEntity> transactions, PortfolioTransactionEntity candidate) {
		List<PortfolioTransactionEntity> sequence = new ArrayList<>(transactions);
		if (sequence.stream().noneMatch(item -> sameTransaction(item, candidate))) sequence.add(candidate);
		sequence.stream()
			.filter(item -> sameAsset(item, candidate))
			.filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE || sameTransaction(item, candidate))
			.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate)
				.thenComparing(PortfolioTransactionEntity::getCreatedAt))
			.map(this::signedQuantity)
			.reduce(BigDecimal.ZERO, (balance, change) -> {
				BigDecimal result = balance.add(change);
				if (result.compareTo(BigDecimal.ZERO) < 0) throw insufficientQuantity();
				return result;
			});
	}

	private BigDecimal availableQuantity(List<PortfolioTransactionEntity> transactions, PortfolioTransactionEntity candidate) {
		return transactions.stream()
			.filter(item -> sameAsset(item, candidate))
			.filter(item -> !sameTransaction(item, candidate))
			.filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE
				|| (item.getStatus() == TransactionStatus.PENDING && item.getType() == TransactionType.SELL))
			.map(this::signedQuantity)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private boolean sameAsset(PortfolioTransactionEntity first, PortfolioTransactionEntity second) {
		return first.getTicker().equalsIgnoreCase(second.getTicker()) && first.getMarket() == second.getMarket();
	}

	private boolean sameTransaction(PortfolioTransactionEntity first, PortfolioTransactionEntity second) {
		return first.getId().equals(second.getId());
	}

	private BigDecimal signedQuantity(PortfolioTransactionEntity transaction) {
		return transaction.getType() == TransactionType.BUY ? transaction.getQuantity() : transaction.getQuantity().negate();
	}

	private PortfolioTransactionConflictException insufficientQuantity() {
		return new PortfolioTransactionConflictException("INSUFFICIENT_ASSET_QUANTITY", "Asset quantity is insufficient for this sale");
	}
}
