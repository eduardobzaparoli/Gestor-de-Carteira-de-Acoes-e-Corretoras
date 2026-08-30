package com.bominvestidor.spring.service.transaction;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;

@Service
public class PortfolioTransactionReconciliationService {
	private final PortfolioTransactionRepository repository;
	private final Clock clock;

	public PortfolioTransactionReconciliationService(PortfolioTransactionRepository repository, Clock clock) {
		this.repository = repository; this.clock = clock;
	}

	public void reconcile(UUID portfolioId) {
		LocalDate today = LocalDate.now(clock); Instant now = clock.instant();
		repository.findAllByPortfolio_Id(portfolioId).stream().filter(item -> item.getStatus() == TransactionStatus.PENDING)
			.filter(item -> !item.getTransactionDate().isAfter(today)).forEach(item -> item.effective(now));
	}
}
