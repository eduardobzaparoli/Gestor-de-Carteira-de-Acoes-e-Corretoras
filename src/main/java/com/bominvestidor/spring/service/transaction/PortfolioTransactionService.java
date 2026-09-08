package com.bominvestidor.spring.service.transaction;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionStates;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionCreateRequest;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionResponse;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionUpdateRequest;
import com.bominvestidor.spring.domain.asset.SelectedAsset;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.exception.PortfolioTransactionConflictException;
import com.bominvestidor.spring.exception.PortfolioTransactionNotFoundException;
import com.bominvestidor.spring.mapper.transaction.PortfolioTransactionMapper;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.portfolio.PortfolioService;
import com.bominvestidor.spring.service.asset.AssetSelectionCache;

@Service
public class PortfolioTransactionService {
	private final PortfolioService portfolioService;
	private final PortfolioTransactionRepository repository;
	private final PortfolioTransactionMapper mapper;
	private final PortfolioTransactionReconciliationService reconciliationService;
	private final PortfolioTransactionBalanceService balanceService;
	private final AssetSelectionCache selectionCache;
	private final Clock clock;
	public PortfolioTransactionService(PortfolioService portfolioService, PortfolioTransactionRepository repository,
			PortfolioTransactionMapper mapper, PortfolioTransactionReconciliationService reconciliationService,
			PortfolioTransactionBalanceService balanceService, AssetSelectionCache selectionCache, Clock clock) { this.portfolioService=portfolioService; this.repository=repository; this.mapper=mapper; this.reconciliationService=reconciliationService; this.balanceService=balanceService; this.selectionCache=selectionCache; this.clock=clock; }

	@Transactional
	public PortfolioTransactionResponse create(UUID ownerId, UUID portfolioId, PortfolioTransactionCreateRequest request) {
		PortfolioEntity portfolio = portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		SelectedAsset asset = selectionCache.find(request.assetSelectionId(), ownerId, portfolioId)
				.orElseThrow(() -> conflict("ASSET_SELECTION_EXPIRED", "Asset selection is expired"));
		Instant now = clock.instant();
		TransactionStatus status = request.transactionDate().isAfter(LocalDate.now(clock)) ? TransactionStatus.PENDING : TransactionStatus.EFFECTIVE;
		PortfolioTransactionEntity entity = new PortfolioTransactionEntity(UUID.randomUUID(), portfolio, asset.ticker().trim().toUpperCase(Locale.ROOT),
				asset.name().trim(), asset.market(), asset.assetType(), asset.currency().trim().toUpperCase(Locale.ROOT), request.type(),
				status, request.transactionDate(), request.quantity(), request.unitPrice(), request.costs() == null ? BigDecimal.ZERO : request.costs(), now, now);
		if (request.type() == TransactionType.SELL) balanceService.validateSale(
				repository.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId), entity);
		PortfolioTransactionResponse response = mapper.toResponse(repository.save(entity));
		selectionCache.remove(request.assetSelectionId());
		return response;
	}

	@Transactional
	public List<PortfolioTransactionResponse> findAll(UUID ownerId, UUID portfolioId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		return repository.findAllByPortfolio_IdOrderByTransactionDateDescCreatedAtDesc(portfolioId).stream().map(mapper::toResponse).toList();
	}

	@Transactional
	public PortfolioTransactionResponse update(UUID ownerId, UUID portfolioId, UUID transactionId,
			PortfolioTransactionUpdateRequest request) {
		portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		PortfolioTransactionEntity transaction = repository.findByIdAndPortfolio_Id(transactionId, portfolioId)
				.orElseThrow(PortfolioTransactionNotFoundException::new);
		if (transaction.getStatus() != TransactionStatus.PENDING) {
			throw conflict("TRANSACTION_CANNOT_BE_EDITED", "Only pending transactions can be edited");
		}
		Instant now = clock.instant();
		transaction.updatePending(request.type(), request.transactionDate(), request.quantity(), request.unitPrice(),
				request.costs() == null ? BigDecimal.ZERO : request.costs(), now);
		var transactions = repository.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId);
		if (transaction.getType() == TransactionType.SELL) {
			balanceService.validateSale(transactions, transaction);
		}
		if (!transaction.getTransactionDate().isAfter(LocalDate.now(clock))) {
			transaction.effective(now);
		}
		return mapper.toResponse(transaction);
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

	private PortfolioTransactionConflictException conflict(String code, String message) {
		return new PortfolioTransactionConflictException(code, message);
	}

}
