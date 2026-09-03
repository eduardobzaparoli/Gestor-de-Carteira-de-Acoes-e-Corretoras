package com.bominvestidor.spring.service.evolution;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetKey;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPrice;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;
import com.bominvestidor.spring.domain.evolution.PortfolioValueEvolutionCalculator;
import com.bominvestidor.spring.domain.evolution.PortfolioValueEvolutionPoint;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.dto.evolution.PortfolioValueEvolutionPointResponse;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;
import com.bominvestidor.spring.exception.AssetQuoteUnavailableException;
import com.bominvestidor.spring.exception.ExchangeRateUnavailableException;
import com.bominvestidor.spring.exception.HistoricalPriceUnavailableException;
import com.bominvestidor.spring.mapper.evolution.PortfolioValueEvolutionMapper;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.asset.AssetSearchService;
import com.bominvestidor.spring.service.exchange.ExchangeRateService;
import com.bominvestidor.spring.service.portfolio.PortfolioService;
import com.bominvestidor.spring.service.transaction.PortfolioTransactionReconciliationService;

@Service
public class PortfolioValueEvolutionService {
	private final PortfolioService portfolioService;
	private final PortfolioTransactionReconciliationService reconciliationService;
	private final PortfolioTransactionRepository transactions;
	private final HistoricalAssetPriceService historicalPrices;
	private final AssetSearchService assetSearch;
	private final ExchangeRateService exchangeRates;
	private final PortfolioValueEvolutionMapper mapper;
	private final Clock clock;
	private final int windowDays;
	private final PortfolioValueEvolutionCalculator calculator = new PortfolioValueEvolutionCalculator();

	public PortfolioValueEvolutionService(PortfolioService portfolioService, PortfolioTransactionReconciliationService reconciliationService,
			PortfolioTransactionRepository transactions, HistoricalAssetPriceService historicalPrices, AssetSearchService assetSearch,
			ExchangeRateService exchangeRates, PortfolioValueEvolutionMapper mapper, Clock clock,
			BrokerageIntegrationProperties properties) {
		this.portfolioService = portfolioService; this.reconciliationService = reconciliationService; this.transactions = transactions;
		this.historicalPrices = historicalPrices; this.assetSearch = assetSearch; this.exchangeRates = exchangeRates; this.mapper = mapper; this.clock = clock;
		this.windowDays = properties.getHistoricalPriceWindowDays();
	}

	@Transactional
	public List<PortfolioValueEvolutionPointResponse> find(UUID ownerId, UUID portfolioId) {
		portfolioService.ownedPortfolio(ownerId, portfolioId);
		reconciliationService.reconcile(portfolioId);
		LocalDate today = LocalDate.now(clock);
		LocalDate start = today.minusDays(Math.max(1, windowDays) - 1L);
		List<PortfolioTransactionEntity> log = transactions.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId);
		List<HistoricalAssetKey> historicalAssets = heldDuringWindow(log, start, today.minusDays(1));
		Map<HistoricalAssetKey, BigDecimal> currentQuantities = quantitiesAt(log, today);
		boolean hasCurrentCustody = currentQuantities.values().stream().anyMatch(value -> value.signum() > 0);
		if (historicalAssets.isEmpty() && !hasCurrentCustody) return List.of();
		Map<HistoricalAssetKey, HistoricalAssetPriceSeries> series = new LinkedHashMap<>();
		for (HistoricalAssetKey asset : historicalAssets) series.put(asset, historicalPrices.find(asset.market(), asset.ticker(), asset.currency(), start, today.minusDays(1)));
		List<LocalDate> historicalDates = series.values().stream().flatMap(value -> value.prices().keySet().stream())
				.filter(date -> !date.isBefore(start) && date.isBefore(today)).distinct().sorted().toList();
		Map<HistoricalAssetKey, HistoricalAssetPriceSeries> allPrices = withCurrentPrices(series, currentQuantities, today);
		List<LocalDate> dates = new ArrayList<>(historicalDates);
		if (hasCurrentCustody) dates.add(today);
		Map<UUID, ExchangeRate> purchaseRates = purchaseRates(log, today);
		Map<LocalDate, ExchangeRate> pointRates = pointRates(log, dates);
		try {
			List<PortfolioValueEvolutionPoint> values = calculator.calculate(log, dates, allPrices, purchaseRates, pointRates);
			return mapper.toResponse(values);
		} catch (IllegalArgumentException exception) { throw new HistoricalPriceUnavailableException(); }
	}

	private Map<HistoricalAssetKey, HistoricalAssetPriceSeries> withCurrentPrices(Map<HistoricalAssetKey, HistoricalAssetPriceSeries> source,
			Map<HistoricalAssetKey, BigDecimal> quantities, LocalDate today) {
		Map<HistoricalAssetKey, HistoricalAssetPriceSeries> result = new LinkedHashMap<>();
		LinkedHashSet<HistoricalAssetKey> keys = new LinkedHashSet<>(source.keySet());
		quantities.forEach((key, quantity) -> { if (quantity.signum() > 0) keys.add(key); });
		for (HistoricalAssetKey key : keys) {
			HistoricalAssetPriceSeries historical = source.get(key);
			var prices = historical == null ? new java.util.TreeMap<LocalDate, HistoricalAssetPrice>() : new java.util.TreeMap<>(historical.prices());
			if (quantities.getOrDefault(key, BigDecimal.ZERO).signum() > 0) {
				AssetQuote quote = assetSearch.findQuote(key.market(), key.ticker()).filter(value -> value.price() != null
						&& value.price().signum() > 0 && key.currency().equalsIgnoreCase(value.currency()))
						.orElseThrow(AssetQuoteUnavailableException::new);
				prices.put(today, new HistoricalAssetPrice(today, quote.price()));
			}
			result.put(key, new HistoricalAssetPriceSeries(key, prices));
		}
		return result;
	}

	private Map<UUID, ExchangeRate> purchaseRates(List<PortfolioTransactionEntity> log, LocalDate today) {
		Map<UUID, ExchangeRate> result = new LinkedHashMap<>();
		for (PortfolioTransactionEntity item : log) if (item.getStatus() == TransactionStatus.EFFECTIVE && item.getType() == TransactionType.BUY
				&& !item.getTransactionDate().isAfter(today) && !"BRL".equalsIgnoreCase(item.getCurrency())) result.put(item.getId(), rate(item.getTransactionDate()));
		return result;
	}
	private Map<LocalDate, ExchangeRate> pointRates(List<PortfolioTransactionEntity> log, List<LocalDate> dates) {
		Map<LocalDate, ExchangeRate> result = new LinkedHashMap<>();
		for (LocalDate date : dates) if (hasUsdCustody(log, date)) result.put(date, rate(date));
		return result;
	}
	private ExchangeRate rate(LocalDate date) {
		return exchangeRates.find("USD", "BRL", date).filter(value -> value.rate() != null && value.rate().signum() > 0)
				.orElseThrow(ExchangeRateUnavailableException::new);
	}
	private List<HistoricalAssetKey> heldDuringWindow(List<PortfolioTransactionEntity> log, LocalDate start, LocalDate end) {
		LinkedHashSet<HistoricalAssetKey> held = new LinkedHashSet<>();
		Map<HistoricalAssetKey, BigDecimal> quantities = new LinkedHashMap<>();
		for (PortfolioTransactionEntity item : effective(log)) {
			if (item.getTransactionDate().isAfter(end)) break;
			HistoricalAssetKey key = new HistoricalAssetKey(item.getMarket(), item.getTicker(), item.getCurrency());
			if (!item.getTransactionDate().isBefore(start) && quantities.getOrDefault(key, BigDecimal.ZERO).signum() > 0) held.add(key);
			BigDecimal signed = item.getType() == TransactionType.BUY ? item.getQuantity() : item.getQuantity().negate();
			quantities.merge(key, signed, BigDecimal::add);
			if (!item.getTransactionDate().isBefore(start) && quantities.get(key).signum() > 0) held.add(key);
		}
		quantities.forEach((key, quantity) -> { if (quantity.signum() > 0) held.add(key); });
		return List.copyOf(held);
	}
	private Map<HistoricalAssetKey, BigDecimal> quantitiesAt(List<PortfolioTransactionEntity> log, LocalDate date) {
		Map<HistoricalAssetKey, BigDecimal> result = new LinkedHashMap<>();
		for (PortfolioTransactionEntity item : effective(log)) {
			if (item.getTransactionDate().isAfter(date)) break;
			HistoricalAssetKey key = new HistoricalAssetKey(item.getMarket(), item.getTicker(), item.getCurrency());
			result.merge(key, item.getType() == TransactionType.BUY ? item.getQuantity() : item.getQuantity().negate(), BigDecimal::add);
		}
		return result;
	}
	private boolean hasUsdCustody(List<PortfolioTransactionEntity> log, LocalDate date) {
		return quantitiesAt(log, date).entrySet().stream().anyMatch(entry -> "USD".equals(entry.getKey().currency()) && entry.getValue().signum() > 0);
	}
	private List<PortfolioTransactionEntity> effective(List<PortfolioTransactionEntity> log) {
		return log.stream().filter(item -> item.getStatus() == TransactionStatus.EFFECTIVE)
				.sorted(Comparator.comparing(PortfolioTransactionEntity::getTransactionDate).thenComparing(PortfolioTransactionEntity::getCreatedAt)).toList();
	}
}
