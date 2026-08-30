package com.bominvestidor.spring.mapper.valuation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.valuation.PortfolioCurrencySummary;
import com.bominvestidor.spring.domain.valuation.PortfolioMarketValuation;
import com.bominvestidor.spring.domain.valuation.PortfolioValuationPosition;
import com.bominvestidor.spring.dto.valuation.PortfolioCurrencySummaryResponse;
import com.bominvestidor.spring.dto.valuation.PortfolioMarketValuationResponse;
import com.bominvestidor.spring.dto.valuation.PortfolioValuationPositionResponse;

@Component
public class PortfolioMarketValuationMapper {
	public PortfolioMarketValuationResponse toResponse(PortfolioMarketValuation valuation) {
		return new PortfolioMarketValuationResponse(valuation.positions().stream().map(this::toResponse).toList(),
				valuation.currencySummaries().stream().map(this::toResponse).toList());
	}
	private PortfolioValuationPositionResponse toResponse(PortfolioValuationPosition value) {
		return new PortfolioValuationPositionResponse(value.ticker(), value.assetName(), value.market(), value.assetType(), value.currency(),
				value.quantity(), decimal(value.averagePrice()), decimal(value.custodyCost()), decimal(value.currentPrice()),
				decimal(value.marketValue()), decimal(value.unrealizedGain()), decimal(value.returnPercentage()), decimal(value.allocationPercentage()));
	}
	private PortfolioCurrencySummaryResponse toResponse(PortfolioCurrencySummary value) {
		return new PortfolioCurrencySummaryResponse(value.currency(), decimal(value.investedValue()), decimal(value.marketValue()),
				decimal(value.unrealizedGain()), decimal(value.returnPercentage()));
	}
	private BigDecimal decimal(BigDecimal value) {
		BigDecimal rounded = value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
		return rounded.scale() < 0 ? rounded.setScale(0) : rounded;
	}
}
