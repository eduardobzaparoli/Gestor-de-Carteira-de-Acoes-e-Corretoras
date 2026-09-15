package com.bominvestidor.spring.integration.income;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.income.IncomeProviderEvent;

public interface IncomeEventProviderStrategy {
	AssetMarket market();
	List<IncomeProviderEvent> findEvents(String ticker);

	default IncomeProviderBatchResult findEvents(Set<String> tickers) {
		Map<String, List<IncomeProviderEvent>> events = new LinkedHashMap<>();
		Map<String, com.bominvestidor.spring.exception.AssetProviderUnavailableException> failures = new LinkedHashMap<>();
		for (String ticker : tickers) {
			try { events.put(ticker, List.copyOf(findEvents(ticker))); }
			catch (com.bominvestidor.spring.exception.AssetProviderUnavailableException exception) { failures.put(ticker, exception); }
		}
		return new IncomeProviderBatchResult(events, failures);
	}
}
