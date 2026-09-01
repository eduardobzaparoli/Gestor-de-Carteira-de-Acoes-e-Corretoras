package com.bominvestidor.spring.domain.valuation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;

public class PortfolioValuationCalculator {
	private static final MathContext CALCULATION_CONTEXT = MathContext.DECIMAL128;
	private static final BigDecimal HUNDRED = new BigDecimal("100");

	public PortfolioMarketValuation calculate(List<PortfolioPositionResponse> positions, Map<PositionKey, AssetQuote> quotes,
			Map<String, ExchangeRate> exchangeRates) {
		return calculate(positions, quotes, exchangeRates, new PortfolioHistoricalCost(positions.stream()
				.map(PortfolioPositionResponse::custodyCost).reduce(BigDecimal.ZERO, (left, right) -> left.add(right, CALCULATION_CONTEXT)), List.of()));
	}
	public PortfolioMarketValuation calculate(List<PortfolioPositionResponse> positions, Map<PositionKey, AssetQuote> quotes,
			Map<String, ExchangeRate> exchangeRates, PortfolioHistoricalCost historicalCost) {
		List<Draft> drafts = positions.stream().map(position -> draft(position, quote(quotes, position))).toList();
		Map<String, Totals> totalsByCurrency = new LinkedHashMap<>();
		for (Draft draft : drafts) totalsByCurrency.merge(draft.currency(), new Totals(draft.custodyCost(), draft.marketValue()), Totals::add);
		List<PortfolioValuationPosition> valued = drafts.stream().map(draft -> position(draft, totalsByCurrency.get(draft.currency())))
				.toList();
		List<PortfolioCurrencySummary> summaries = totalsByCurrency.entrySet().stream()
				.sorted(Map.Entry.comparingByKey()).map(entry -> summary(entry.getKey(), entry.getValue())).toList();
		return new PortfolioMarketValuation(valued, summaries, consolidated(drafts, exchangeRates, historicalCost));
	}
	private PortfolioConsolidatedSummary consolidated(List<Draft> drafts, Map<String, ExchangeRate> exchangeRates, PortfolioHistoricalCost historicalCost) {
		if (drafts.isEmpty()) return null;
		BigDecimal marketValue = BigDecimal.ZERO;
		for (Draft draft : drafts) {
			if ("BRL".equalsIgnoreCase(draft.currency())) marketValue = marketValue.add(draft.marketValue(), CALCULATION_CONTEXT);
			else marketValue = marketValue.add(draft.marketValue().multiply(exchangeRate(exchangeRates, draft.currency()).rate(), CALCULATION_CONTEXT), CALCULATION_CONTEXT);
		}
		List<ExchangeRate> rates = exchangeRates.values().stream().sorted(Comparator.comparing(ExchangeRate::sourceCurrency)).toList();
		BigDecimal gain = marketValue.subtract(historicalCost.investedValue(), CALCULATION_CONTEXT);
		return new PortfolioConsolidatedSummary("BRL", historicalCost.investedValue(), marketValue, gain,
				percentage(gain, historicalCost.investedValue()), rates, historicalCost.exchangeRates());
	}
	private ExchangeRate exchangeRate(Map<String, ExchangeRate> exchangeRates, String currency) {
		ExchangeRate rate = exchangeRates.get(currency);
		if (rate == null) throw new IllegalArgumentException("Missing exchange rate");
		return rate;
	}

	private AssetQuote quote(Map<PositionKey, AssetQuote> quotes, PortfolioPositionResponse position) {
		AssetQuote quote = quotes.get(new PositionKey(position.market(), position.ticker()));
		if (quote == null) throw new IllegalArgumentException("Missing quote");
		return quote;
	}
	private Draft draft(PortfolioPositionResponse position, AssetQuote quote) {
		BigDecimal marketValue = position.quantity().multiply(quote.price(), CALCULATION_CONTEXT);
		BigDecimal gain = marketValue.subtract(position.custodyCost(), CALCULATION_CONTEXT);
		return new Draft(position, marketValue, gain, percentage(gain, position.custodyCost()));
	}
	private PortfolioValuationPosition position(Draft draft, Totals totals) {
		PortfolioPositionResponse source = draft.position();
		return new PortfolioValuationPosition(source.ticker(), source.assetName(), source.market(), source.assetType(),
				source.currency(), source.quantity(), source.averagePrice(), source.custodyCost(), draft.currentPrice(),
				draft.marketValue(), draft.gain(), draft.returnPercentage(), percentage(draft.marketValue(), totals.marketValue()));
	}
	private PortfolioCurrencySummary summary(String currency, Totals totals) {
		BigDecimal gain = totals.marketValue().subtract(totals.investedValue(), CALCULATION_CONTEXT);
		return new PortfolioCurrencySummary(currency, totals.investedValue(), totals.marketValue(), gain,
				percentage(gain, totals.investedValue()));
	}
	private BigDecimal percentage(BigDecimal value, BigDecimal base) {
		return base.signum() == 0 ? BigDecimal.ZERO : value.multiply(HUNDRED).divide(base, 16, RoundingMode.HALF_UP);
	}

	public record PositionKey(com.bominvestidor.spring.domain.asset.AssetMarket market, String ticker) { }
	private record Draft(PortfolioPositionResponse position, BigDecimal marketValue, BigDecimal gain, BigDecimal returnPercentage) {
		BigDecimal currentPrice() { return marketValue.divide(position.quantity(), CALCULATION_CONTEXT); }
		String currency() { return position.currency(); }
		BigDecimal custodyCost() { return position.custodyCost(); }
	}
	private record Totals(BigDecimal investedValue, BigDecimal marketValue) {
		Totals add(Totals other) { return new Totals(investedValue.add(other.investedValue(), CALCULATION_CONTEXT), marketValue.add(other.marketValue(), CALCULATION_CONTEXT)); }
	}
}
