package com.bominvestidor.spring.service.position;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.position.PortfolioPositionCalculator;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;
import com.bominvestidor.spring.mapper.position.PortfolioPositionMapper;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.portfolio.PortfolioService;
import com.bominvestidor.spring.service.transaction.PortfolioTransactionReconciliationService;

@Service
public class PortfolioPositionService {
	private final PortfolioService portfolioService;
	private final PortfolioTransactionReconciliationService reconciliationService;
	private final PortfolioTransactionRepository transactionRepository;
	private final PortfolioPositionMapper mapper;
	private final PortfolioPositionCalculator calculator = new PortfolioPositionCalculator();

	public PortfolioPositionService(PortfolioService portfolioService, PortfolioTransactionReconciliationService reconciliationService,
			PortfolioTransactionRepository transactionRepository, PortfolioPositionMapper mapper) {
		this.portfolioService = portfolioService; this.reconciliationService = reconciliationService;
		this.transactionRepository = transactionRepository; this.mapper = mapper;
	}

	@Transactional
	public List<PortfolioPositionResponse> findAll(UUID ownerId, UUID portfolioId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		return calculator.calculate(transactionRepository.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId))
			.stream().map(mapper::toResponse).toList();
	}
}
