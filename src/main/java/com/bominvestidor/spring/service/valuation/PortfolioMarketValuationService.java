package com.bominvestidor.spring.service.valuation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.valuation.PortfolioMarketValuation;
import com.bominvestidor.spring.domain.valuation.PortfolioValuationCalculator;
import com.bominvestidor.spring.domain.valuation.PortfolioValuationCalculator.PositionKey;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;
import com.bominvestidor.spring.dto.valuation.PortfolioMarketValuationResponse;
import com.bominvestidor.spring.exception.AssetQuoteUnavailableException;
import com.bominvestidor.spring.mapper.valuation.PortfolioMarketValuationMapper;
import com.bominvestidor.spring.service.asset.AssetSearchService;
import com.bominvestidor.spring.service.position.PortfolioPositionService;

@Service
public class PortfolioMarketValuationService {
	private final PortfolioPositionService positionService;
	private final AssetSearchService assetSearchService;
	private final PortfolioMarketValuationMapper mapper;
	private final PortfolioValuationCalculator calculator = new PortfolioValuationCalculator();

	public PortfolioMarketValuationService(PortfolioPositionService positionService, AssetSearchService assetSearchService,
			PortfolioMarketValuationMapper mapper) {
		this.positionService = positionService;
		this.assetSearchService = assetSearchService;
		this.mapper = mapper;
	}

	public PortfolioMarketValuationResponse find(UUID ownerId, UUID portfolioId) {
		List<PortfolioPositionResponse> positions = positionService.findAll(ownerId, portfolioId);
		Map<PositionKey, AssetQuote> quotes = new LinkedHashMap<>();
		for (PortfolioPositionResponse position : positions) {
			AssetQuote quote = assetSearchService.findQuote(position.market(), position.ticker())
					.filter(value -> valid(position, value)).orElseThrow(AssetQuoteUnavailableException::new);
			quotes.put(new PositionKey(position.market(), position.ticker()), quote);
		}
		PortfolioMarketValuation valuation = calculator.calculate(positions, quotes);
		return mapper.toResponse(valuation);
	}

	private boolean valid(PortfolioPositionResponse position, AssetQuote quote) {
		return quote.price() != null && quote.price().signum() > 0 && position.ticker().equalsIgnoreCase(quote.ticker())
				&& position.currency().equalsIgnoreCase(quote.currency());
	}
}
