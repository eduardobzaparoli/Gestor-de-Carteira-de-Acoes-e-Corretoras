package com.bominvestidor.spring.service.income;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.income.IncomeEventCandidate;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventStatus;
import com.bominvestidor.spring.domain.income.IncomeEventStates;
import com.bominvestidor.spring.domain.income.IncomeProviderEvent;
import com.bominvestidor.spring.domain.income.PortfolioIncomeCurrencySummary;
import com.bominvestidor.spring.domain.income.PortfolioIncomeSummary;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.dto.error.FieldErrorResponse;
import com.bominvestidor.spring.dto.income.IncomeEventCandidateResponse;
import com.bominvestidor.spring.dto.income.IncomeEventCandidatesResponse;
import com.bominvestidor.spring.dto.income.IncomeEventConfirmationRequest;
import com.bominvestidor.spring.dto.income.ManualIncomeEventCreateRequest;
import com.bominvestidor.spring.dto.income.PortfolioIncomeEventResponse;
import com.bominvestidor.spring.dto.income.PortfolioIncomeSummaryResponse;
import com.bominvestidor.spring.entity.income.PortfolioIncomeEventEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.exception.ExchangeRateUnavailableException;
import com.bominvestidor.spring.exception.InvalidIncomeEventDataException;
import com.bominvestidor.spring.exception.PortfolioIncomeEventConflictException;
import com.bominvestidor.spring.exception.PortfolioIncomeEventNotFoundException;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategy;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategyResolver;
import com.bominvestidor.spring.mapper.income.PortfolioIncomeEventMapper;
import com.bominvestidor.spring.repository.income.PortfolioIncomeEventRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.exchange.ExchangeRateService;
import com.bominvestidor.spring.service.portfolio.PortfolioService;
import com.bominvestidor.spring.service.asset.AssetIdentityRules;

@Service
public class PortfolioIncomeEventService {
	private final PortfolioService portfolioService;
	private final PortfolioTransactionRepository transactionRepository;
	private final PortfolioIncomeEventRepository repository;
	private final IncomeEventProviderStrategyResolver providerResolver;
	private final IncomeEventCandidateCache candidateCache;
	private final IncomeProviderEventCache providerEventCache;
	private final PortfolioIncomeEventMapper mapper;
	private final PortfolioIncomeEventReconciliationService reconciliationService;
	private final ExchangeRateService exchangeRateService;
	private final Clock clock;

	public PortfolioIncomeEventService(PortfolioService portfolioService, PortfolioTransactionRepository transactionRepository,
			PortfolioIncomeEventRepository repository, IncomeEventProviderStrategyResolver providerResolver,
			IncomeEventCandidateCache candidateCache, IncomeProviderEventCache providerEventCache, PortfolioIncomeEventMapper mapper,
			PortfolioIncomeEventReconciliationService reconciliationService, ExchangeRateService exchangeRateService, Clock clock) {
		this.portfolioService=portfolioService; this.transactionRepository=transactionRepository; this.repository=repository;
		this.providerResolver=providerResolver; this.candidateCache=candidateCache; this.providerEventCache=providerEventCache; this.mapper=mapper;
		this.reconciliationService=reconciliationService; this.exchangeRateService=exchangeRateService; this.clock=clock;
	}

	@Transactional
	public IncomeEventCandidatesResponse findCandidates(UUID ownerId, UUID portfolioId, AssetMarket market) {
		portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		IncomeEventProviderStrategy provider = providerResolver.resolve(market);
		if (provider == null) return new IncomeEventCandidatesResponse(List.of(), clock.instant(), false, List.of());
		List<PortfolioTransactionEntity> transactions = transactionRepository.findAllByPortfolio_Id(portfolioId);
		Map<String, PortfolioTransactionEntity> assets = transactions.stream()
			.filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE && item.getType() == TransactionType.BUY && item.getMarket() == market)
			.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate).reversed()
				.thenComparing(PortfolioTransactionEntity::getCreatedAt, Comparator.reverseOrder()))
			.collect(java.util.stream.Collectors.toMap(item -> item.getTicker().toUpperCase(Locale.ROOT), item -> item, (first, ignored) -> first, LinkedHashMap::new));
		if (assets.isEmpty()) return new IncomeEventCandidatesResponse(List.of(), clock.instant(), false, List.of());
		IncomeProviderEventCache.Resolution resolution = providerEventCache.resolve(market, assets.keySet(), provider);
		List<IncomeEventCandidateResponse> candidates = resolution.events().entrySet().stream()
				.flatMap(item -> item.getValue().stream().map(event -> candidate(portfolioId, assets.get(item.getKey()), transactions, event)))
				.sorted(Comparator.comparing(IncomeEventCandidate::paymentDate).reversed().thenComparing(IncomeEventCandidate::ticker))
				.map(candidate -> toCandidateResponse(ownerId, portfolioId, candidate)).toList();
		return new IncomeEventCandidatesResponse(candidates, resolution.updatedAt(), resolution.stale(), resolution.warnings());
	}

	@Transactional
	public PortfolioIncomeEventResponse confirm(UUID ownerId, UUID portfolioId, IncomeEventConfirmationRequest request) {
		PortfolioEntity portfolio = portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		IncomeEventCandidate candidate = candidateCache.find(request.candidateId(), ownerId, portfolioId)
			.orElseThrow(() -> conflict("INCOME_CANDIDATE_EXPIRED", "Income event candidate is expired; synchronize again"));
		if (!candidate.confirmable()) throw conflict("INCOME_EVENT_CANNOT_BE_CONFIRMED", "Income event cannot be confirmed");
		if (repository.existsByPortfolio_IdAndSourceAndEventKey(portfolioId, candidate.source(), candidate.eventKey()))
			throw conflict("INCOME_EVENT_ALREADY_RECORDED", "Income event is already recorded");
		if (candidate.expectedAmount().compareTo(request.receivedAmount()) != 0 && blank(request.adjustmentReason()))
			throw invalid("adjustmentReason", "Adjustment reason is required when received amount differs from expected amount");
		PortfolioIncomeEventEntity entity = newEntity(portfolio, candidate.ticker(), candidate.assetName(), candidate.market(), candidate.assetType(),
				candidate.currency(), candidate.type(), candidate.source(), candidate.eventKey(), candidate.eligibilityDate(), candidate.paymentDate(),
				candidate.eligibleQuantity(), candidate.unitAmount(), candidate.expectedAmount(), request.receivedAmount(), request.adjustmentReason(), null);
		try { entity = repository.save(entity); } catch (DataIntegrityViolationException exception) { throw conflict("INCOME_EVENT_ALREADY_RECORDED", "Income event is already recorded"); }
		candidateCache.remove(request.candidateId());
		return mapper.toResponse(entity);
	}

	@Transactional
	public PortfolioIncomeEventResponse createManual(UUID ownerId, UUID portfolioId, ManualIncomeEventCreateRequest request) {
		PortfolioEntity portfolio = portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		String ticker = request.ticker().trim().toUpperCase(Locale.ROOT);
		PortfolioTransactionEntity acquired = transactionRepository.findAllByPortfolio_Id(portfolioId).stream()
				.filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE && item.getType() == TransactionType.BUY && item.getTicker().equalsIgnoreCase(ticker))
				.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate).reversed()
						.thenComparing(PortfolioTransactionEntity::getCreatedAt, Comparator.reverseOrder()))
				.findFirst().orElseThrow(() -> conflict("ASSET_NOT_ACQUIRED", "Asset must have at least one effective purchase in this portfolio"));
		if (!AssetIdentityRules.hasCompatibleCurrency(acquired.getMarket(), acquired.getCurrency()))
			throw conflict("ASSET_NOT_ACQUIRED", "Asset has an incompatible market and currency");
		String key = "manual|" + ticker + "|" + request.type() + "|" + request.paymentDate() + "|" + request.eligibilityDate() + "|"
				+ request.receivedAmount().stripTrailingZeros().toPlainString() + "|" + request.unitAmount();
		if (repository.existsByPortfolio_IdAndSourceAndEventKey(portfolioId, IncomeEventSource.MANUAL, key))
			throw conflict("INCOME_EVENT_ALREADY_RECORDED", "Income event is already recorded");
		PortfolioIncomeEventEntity entity = newEntity(portfolio, ticker, acquired.getAssetName(), acquired.getMarket(), acquired.getAssetType(), acquired.getCurrency(),
				request.type(), IncomeEventSource.MANUAL, key, request.eligibilityDate(), request.paymentDate(), request.eligibleQuantity(), request.unitAmount(),
				null, request.receivedAmount(), null, request.notes());
		try { return mapper.toResponse(repository.save(entity)); } catch (DataIntegrityViolationException exception) { throw conflict("INCOME_EVENT_ALREADY_RECORDED", "Income event is already recorded"); }
	}

	@Transactional
	public List<PortfolioIncomeEventResponse> findAll(UUID ownerId, UUID portfolioId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId); reconciliationService.reconcile(portfolioId);
		return repository.findAllByPortfolio_IdOrderByPaymentDateDescCreatedAtDesc(portfolioId).stream().map(mapper::toResponse).toList();
	}

	@Transactional
	public void cancel(UUID ownerId, UUID portfolioId, UUID incomeEventId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId); reconciliationService.reconcile(portfolioId);
		PortfolioIncomeEventEntity entity = repository.findByIdAndPortfolio_Id(incomeEventId, portfolioId).orElseThrow(PortfolioIncomeEventNotFoundException::new);
		if (!IncomeEventStates.from(entity.getStatus()).canCancel()) throw conflict("INCOME_EVENT_CANNOT_BE_CANCELLED", "Only pending income events can be cancelled");
		entity.cancel(clock.instant());
	}

	@Transactional
	public PortfolioIncomeSummaryResponse summary(UUID ownerId, UUID portfolioId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId); reconciliationService.reconcile(portfolioId);
		List<PortfolioIncomeEventEntity> events = repository.findAllByPortfolio_IdAndStatus(portfolioId, IncomeEventStatus.EFFECTIVE);
		Map<String, BigDecimal> totals = new java.util.TreeMap<>();
		Map<String, ExchangeRate> rates = new LinkedHashMap<>();
		BigDecimal brlTotal = BigDecimal.ZERO;
		for (PortfolioIncomeEventEntity event : events) {
			totals.merge(event.getCurrency(), event.getReceivedAmount(), BigDecimal::add);
			if ("BRL".equals(event.getCurrency())) brlTotal = brlTotal.add(event.getReceivedAmount());
			else if ("USD".equals(event.getCurrency())) {
				ExchangeRate rate = exchangeRateService.find("USD", "BRL", event.getPaymentDate())
					.orElseThrow(ExchangeRateUnavailableException::new);
				rates.putIfAbsent(rate.referenceDate() + "|" + rate.sourceCurrency() + "|" + rate.targetCurrency(), rate);
				brlTotal = brlTotal.add(event.getReceivedAmount().multiply(rate.rate()));
			}
		}
		List<PortfolioIncomeCurrencySummary> currencySummaries = totals.entrySet().stream().map(item -> new PortfolioIncomeCurrencySummary(item.getKey(), item.getValue())).toList();
		return mapper.toSummaryResponse(new PortfolioIncomeSummary(currencySummaries, "BRL", brlTotal, List.copyOf(rates.values())));
	}

	private IncomeEventCandidate candidate(UUID portfolioId, PortfolioTransactionEntity asset, List<PortfolioTransactionEntity> transactions, IncomeProviderEvent event) {
		boolean futureEligibility = event.eligibilityDate().isAfter(LocalDate.now(clock));
		BigDecimal quantity = futureEligibility ? null : IncomeEligibilityCalculator.quantityAt(transactions, asset.getTicker(), asset.getMarket(), event.eligibilityDate(), asset.getMarket() == AssetMarket.BR);
		BigDecimal expected = quantity == null ? null : quantity.multiply(event.unitAmount());
		boolean recorded = repository.existsByPortfolio_IdAndSourceAndEventKey(portfolioId, event.source(), event.eventKey());
		boolean confirmable = !futureEligibility && quantity != null && quantity.signum() > 0 && !recorded;
		return new IncomeEventCandidate(event.eventKey(), asset.getTicker(), asset.getAssetName(), asset.getMarket(), asset.getAssetType(), asset.getCurrency(), event.type(), event.source(), event.unitAmount(), event.eligibilityDate(), event.paymentDate(), quantity, expected, confirmable, recorded);
	}
	private IncomeEventCandidateResponse toCandidateResponse(UUID ownerId, UUID portfolioId, IncomeEventCandidate candidate) {
		UUID id = candidateCache.store(ownerId, portfolioId, candidate);
		return new IncomeEventCandidateResponse(id, candidate.ticker(), candidate.assetName(), candidate.market(), candidate.assetType(), candidate.currency(), candidate.type(), candidate.source(), candidate.unitAmount(), candidate.eligibilityDate(), candidate.paymentDate(), candidate.eligibleQuantity(), candidate.expectedAmount(), candidate.confirmable(), candidate.alreadyRecorded());
	}
	private PortfolioIncomeEventEntity newEntity(PortfolioEntity portfolio, String ticker, String assetName, AssetMarket market,
			com.bominvestidor.spring.domain.asset.AssetType assetType, String currency, com.bominvestidor.spring.domain.income.IncomeEventType type,
			IncomeEventSource source, String eventKey, LocalDate eligibilityDate, LocalDate paymentDate, BigDecimal eligibleQuantity,
			BigDecimal unitAmount, BigDecimal expectedAmount, BigDecimal receivedAmount, String adjustmentReason, String notes) {
		Instant now = clock.instant(); IncomeEventStatus status = paymentDate.isAfter(LocalDate.now(clock)) ? IncomeEventStatus.PENDING : IncomeEventStatus.EFFECTIVE;
		return new PortfolioIncomeEventEntity(UUID.randomUUID(), portfolio, ticker, assetName, market, assetType, currency, type, source, eventKey,
				status, eligibilityDate, paymentDate, eligibleQuantity, unitAmount, expectedAmount, receivedAmount, blank(adjustmentReason) ? null : adjustmentReason.trim(), blank(notes) ? null : notes.trim(), now, now);
	}
	private boolean blank(String value) { return value == null || value.isBlank(); }
	private InvalidIncomeEventDataException invalid(String field, String message) { return new InvalidIncomeEventDataException(List.of(new FieldErrorResponse(field, message))); }
	private PortfolioIncomeEventConflictException conflict(String code, String message) { return new PortfolioIncomeEventConflictException(code, message); }
}
