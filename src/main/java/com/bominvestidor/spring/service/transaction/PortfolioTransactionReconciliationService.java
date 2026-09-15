package com.bominvestidor.spring.service.transaction;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.exception.PortfolioNotFoundException;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;

@Service
public class PortfolioTransactionReconciliationService {
	private final PortfolioTransactionRepository repository;
	private final PortfolioRepository portfolioRepository;
	private final PortfolioTransactionBalanceService balanceService;
	private final Clock clock;

	public PortfolioTransactionReconciliationService(PortfolioTransactionRepository repository, PortfolioRepository portfolioRepository,
			PortfolioTransactionBalanceService balanceService, Clock clock) {
		this.repository = repository; this.portfolioRepository = portfolioRepository; this.balanceService = balanceService; this.clock = clock;
	}

	@Transactional
	public void reconcile(UUID portfolioId) {
		portfolioRepository.findByIdForUpdate(portfolioId).orElseThrow(PortfolioNotFoundException::new);
		LocalDate today = LocalDate.now(clock); Instant now = clock.instant();
		var transactions = repository.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId);
		transactions.stream().filter(item -> item.getStatus() == TransactionStatus.PENDING)
			.filter(item -> !item.getTransactionDate().isAfter(today))
			.sorted(Comparator.comparing(com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity::getTransactionDate)
				.thenComparing(com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity::getCreatedAt))
			.forEach(item -> {
				if (item.getType() == TransactionType.SELL) balanceService.validateSale(transactions, item);
				item.effective(now);
			});
	}
}
