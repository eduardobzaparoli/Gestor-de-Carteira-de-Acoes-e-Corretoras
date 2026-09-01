package com.bominvestidor.spring.mapper.income;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.income.PortfolioIncomeCurrencySummary;
import com.bominvestidor.spring.domain.income.PortfolioIncomeSummary;
import com.bominvestidor.spring.dto.income.PortfolioIncomeCurrencySummaryResponse;
import com.bominvestidor.spring.dto.income.PortfolioIncomeEventResponse;
import com.bominvestidor.spring.dto.income.PortfolioIncomeSummaryResponse;
import com.bominvestidor.spring.dto.valuation.ExchangeRateResponse;
import com.bominvestidor.spring.entity.income.PortfolioIncomeEventEntity;

@Component
public class PortfolioIncomeEventMapper {
	public PortfolioIncomeEventResponse toResponse(PortfolioIncomeEventEntity entity) {
		return new PortfolioIncomeEventResponse(entity.getId(), entity.getTicker(), entity.getAssetName(), entity.getMarket(),
				entity.getAssetType(), entity.getCurrency(), entity.getType(), entity.getSource(), entity.getStatus(),
				entity.getEligibilityDate(), entity.getPaymentDate(), entity.getEligibleQuantity(), entity.getUnitAmount(),
				entity.getExpectedAmount(), entity.getReceivedAmount(), entity.getAdjustmentReason(), entity.getNotes(),
				entity.getCreatedAt(), entity.getUpdatedAt());
	}
	public PortfolioIncomeSummaryResponse toSummaryResponse(PortfolioIncomeSummary summary) {
		return new PortfolioIncomeSummaryResponse(summary.currencySummaries().stream().map(this::toCurrencyResponse).toList(),
				summary.baseCurrency(), summary.consolidatedReceivedAmount(), summary.exchangeRates().stream()
					.map(rate -> new ExchangeRateResponse(rate.sourceCurrency(), rate.targetCurrency(), rate.rate(), rate.referenceDate())).toList());
	}
	private PortfolioIncomeCurrencySummaryResponse toCurrencyResponse(PortfolioIncomeCurrencySummary summary) {
		return new PortfolioIncomeCurrencySummaryResponse(summary.currency(), summary.receivedAmount());
	}
}
