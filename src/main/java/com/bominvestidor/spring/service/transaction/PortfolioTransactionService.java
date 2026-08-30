package com.bominvestidor.spring.service.transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionStates;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionCreateRequest;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionResponse;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.exception.PortfolioTransactionConflictException;
import com.bominvestidor.spring.exception.PortfolioTransactionNotFoundException;
import com.bominvestidor.spring.mapper.transaction.PortfolioTransactionMapper;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

@Service
public class PortfolioTransactionService {
	private final PortfolioService portfolioService;
	private final PortfolioTransactionRepository repository;
	private final PortfolioTransactionMapper mapper;
	private final PortfolioTransactionReconciliationService reconciliationService;
	private final Clock clock;
	public PortfolioTransactionService(PortfolioService portfolioService, PortfolioTransactionRepository repository,
			PortfolioTransactionMapper mapper, PortfolioTransactionReconciliationService reconciliationService, Clock clock) { this.portfolioService=portfolioService; this.repository=repository; this.mapper=mapper; this.reconciliationService=reconciliationService; this.clock=clock; }

	@Transactional
	public PortfolioTransactionResponse create(UUID ownerId, UUID portfolioId, PortfolioTransactionCreateRequest request) {
		PortfolioEntity portfolio = portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		Instant now = clock.instant();
		TransactionStatus status = request.transactionDate().isAfter(LocalDate.now(clock)) ? TransactionStatus.PENDING : TransactionStatus.EFFECTIVE;
		PortfolioTransactionEntity entity = new PortfolioTransactionEntity(UUID.randomUUID(), portfolio, request.ticker().trim().toUpperCase(Locale.ROOT),
				request.assetName().trim(), request.market(), request.assetType(), request.currency().trim().toUpperCase(Locale.ROOT), request.type(),
				status, request.transactionDate(), request.quantity(), request.unitPrice(), request.costs() == null ? BigDecimal.ZERO : request.costs(), now, now);
		if (request.type() == TransactionType.SELL && available(portfolioId, request.ticker(), request.market()).compareTo(request.quantity()) < 0)
			throw conflict("INSUFFICIENT_ASSET_QUANTITY", "Asset quantity is insufficient for this sale");
		if (status == TransactionStatus.EFFECTIVE) validateChronologicalQuantity(portfolioId, entity);
		return mapper.toResponse(repository.save(entity));
	}

	@Transactional
	public List<PortfolioTransactionResponse> findAll(UUID ownerId, UUID portfolioId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		return repository.findAllByPortfolio_IdOrderByTransactionDateDescCreatedAtDesc(portfolioId).stream().map(mapper::toResponse).toList();
	}

	@Transactional
	public void cancel(UUID ownerId, UUID portfolioId, UUID transactionId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		PortfolioTransactionEntity transaction = repository.findByIdAndPortfolio_Id(transactionId, portfolioId).orElseThrow(PortfolioTransactionNotFoundException::new);
		if (!TransactionStates.from(transaction.getStatus()).canCancel())
			throw conflict("TRANSACTION_CANNOT_BE_CANCELLED", "Only pending transactions can be cancelled");
		transaction.cancel(clock.instant());
	}

	private void validateChronologicalQuantity(UUID portfolioId, PortfolioTransactionEntity candidate) {
		List<PortfolioTransactionEntity> transactions = new ArrayList<>(repository.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId));
		transactions.add(candidate);
		BigDecimal quantity = transactions.stream().filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE)
			.filter(item -> item.getTicker().equalsIgnoreCase(candidate.getTicker()) && item.getMarket() == candidate.getMarket())
			.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate).thenComparing(PortfolioTransactionEntity::getCreatedAt))
			.map(item -> item.getType() == TransactionType.BUY ? item.getQuantity() : item.getQuantity().negate())
			.reduce(BigDecimal.ZERO, (balance, change) -> {
				BigDecimal result = balance.add(change);
				if (result.compareTo(BigDecimal.ZERO) < 0) throw conflict("INSUFFICIENT_ASSET_QUANTITY", "Asset quantity is insufficient for this sale");
				return result;
			});
	}

	private BigDecimal available(UUID portfolioId, String ticker, com.bominvestidor.spring.domain.asset.AssetMarket market) {
		return repository.findAllByPortfolio_Id(portfolioId).stream()
			.filter(item -> item.getTicker().equalsIgnoreCase(ticker.trim()) && item.getMarket() == market)
			.filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE || (item.getStatus() == TransactionStatus.PENDING && item.getType() == TransactionType.SELL))
			.map(item -> item.getType() == TransactionType.BUY ? item.getQuantity() : item.getQuantity().negate())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	private PortfolioTransactionConflictException conflict(String code, String message) { return new PortfolioTransactionConflictException(code, message); }
}
