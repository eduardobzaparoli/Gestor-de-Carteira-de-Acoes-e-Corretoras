package com.bominvestidor.spring.service.valuation;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;

import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.domain.valuation.PortfolioHistoricalCost;
import com.bominvestidor.spring.domain.valuation.PortfolioHistoricalCostCalculator;
import com.bominvestidor.spring.domain.valuation.PortfolioMarketValuation;
import com.bominvestidor.spring.domain.valuation.PortfolioValuationCalculator;
import com.bominvestidor.spring.domain.valuation.PortfolioValuationCalculator.PositionKey;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;
import com.bominvestidor.spring.dto.valuation.PortfolioMarketValuationResponse;
import com.bominvestidor.spring.exception.AssetQuoteUnavailableException;
import com.bominvestidor.spring.exception.ExchangeRateUnavailableException;
import com.bominvestidor.spring.mapper.valuation.PortfolioMarketValuationMapper;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.service.asset.AssetSearchService;
import com.bominvestidor.spring.service.exchange.ExchangeRateService;
import com.bominvestidor.spring.service.position.PortfolioPositionService;

@Service
public class PortfolioMarketValuationService {
	private final PortfolioPositionService positionService;
	private final AssetSearchService assetSearchService;
	private final ExchangeRateService exchangeRateService;
	private final PortfolioMarketValuationMapper mapper;
	private final PortfolioTransactionRepository transactionRepository;
	private final Clock clock;
	private final PortfolioValuationCalculator calculator = new PortfolioValuationCalculator();
	private final PortfolioHistoricalCostCalculator historicalCostCalculator = new PortfolioHistoricalCostCalculator();

	public PortfolioMarketValuationService(PortfolioPositionService positionService, AssetSearchService assetSearchService,
			ExchangeRateService exchangeRateService, PortfolioMarketValuationMapper mapper, PortfolioTransactionRepository transactionRepository, Clock clock) {
		this.positionService = positionService;
		this.assetSearchService = assetSearchService;
		this.exchangeRateService = exchangeRateService;
		this.mapper = mapper;
		this.transactionRepository = transactionRepository;
		this.clock = clock;
	}

	public PortfolioMarketValuationResponse find(UUID ownerId, UUID portfolioId) {
		List<PortfolioPositionResponse> positions = positionService.findAll(ownerId, portfolioId);
		Map<PositionKey, AssetQuote> quotes = new LinkedHashMap<>();
		for (PortfolioPositionResponse position : positions) {
			AssetQuote quote = assetSearchService.findQuote(position.market(), position.ticker())
					.filter(value -> valid(position, value)).orElseThrow(AssetQuoteUnavailableException::new);
			quotes.put(new PositionKey(position.market(), position.ticker()), quote);
		}
		Map<String, ExchangeRate> currentRates = exchangeRates(positions);
		PortfolioHistoricalCost historicalCost = historicalCost(portfolioId);
		PortfolioMarketValuation valuation = calculator.calculate(positions, quotes, currentRates, historicalCost);
		return mapper.toResponse(valuation);
	}

	private PortfolioHistoricalCost historicalCost(UUID portfolioId) {
		List<com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity> transactions = transactionRepository
				.findAllByPortfolio_IdOrderByTransactionDateAscCreatedAtAsc(portfolioId);
		Map<UUID, ExchangeRate> rates = new LinkedHashMap<>();
		for (com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity transaction : transactions) {
			if (transaction.getStatus() != TransactionStatus.EFFECTIVE || transaction.getType() != TransactionType.BUY
					|| "BRL".equalsIgnoreCase(transaction.getCurrency())) continue;
			ExchangeRate rate = exchangeRateService.find(transaction.getCurrency(), "BRL", transaction.getTransactionDate())
					.filter(value -> valid(transaction.getCurrency(), value)).orElseThrow(ExchangeRateUnavailableException::new);
			rates.put(transaction.getId(), rate);
		}
		return historicalCostCalculator.calculate(transactions, rates);
	}

	private Map<String, ExchangeRate> exchangeRates(List<PortfolioPositionResponse> positions) {
		Map<String, ExchangeRate> rates = new LinkedHashMap<>();
		LocalDate requestedDate = LocalDate.now(clock);
		for (PortfolioPositionResponse position : positions) {
			String currency = position.currency().toUpperCase(java.util.Locale.ROOT);
			if ("BRL".equals(currency) || rates.containsKey(currency)) continue;
			ExchangeRate rate = exchangeRateService.find(currency, "BRL", requestedDate)
					.filter(value -> valid(currency, value)).orElseThrow(ExchangeRateUnavailableException::new);
			rates.put(currency, rate);
		}
		return rates;
	}

	private boolean valid(PortfolioPositionResponse position, AssetQuote quote) {
		return quote.price() != null && quote.price().signum() > 0 && position.ticker().equalsIgnoreCase(quote.ticker())
				&& position.currency().equalsIgnoreCase(quote.currency());
	}
	private boolean valid(String sourceCurrency, ExchangeRate rate) {
		return rate.rate() != null && rate.rate().signum() > 0 && rate.referenceDate() != null
				&& sourceCurrency.equalsIgnoreCase(rate.sourceCurrency()) && "BRL".equalsIgnoreCase(rate.targetCurrency());
	}
}
