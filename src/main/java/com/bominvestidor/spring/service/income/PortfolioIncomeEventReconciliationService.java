package com.bominvestidor.spring.service.income;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.income.IncomeEventStatus;
import com.bominvestidor.spring.repository.income.PortfolioIncomeEventRepository;

@Service
public class PortfolioIncomeEventReconciliationService {
	private final PortfolioIncomeEventRepository repository;
	private final Clock clock;

	public PortfolioIncomeEventReconciliationService(PortfolioIncomeEventRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	public void reconcile(UUID portfolioId) {
		LocalDate today = LocalDate.now(clock);
		Instant now = clock.instant();
		repository.findAllByPortfolio_IdAndStatus(portfolioId, IncomeEventStatus.PENDING).stream()
			.filter(item -> !item.getPaymentDate().isAfter(today)).forEach(item -> item.effective(now));
	}
}
