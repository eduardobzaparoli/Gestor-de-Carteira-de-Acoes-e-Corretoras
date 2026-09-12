package com.bominvestidor.spring.integration.income;

import java.util.List;
import java.util.Map;

import com.bominvestidor.spring.domain.income.IncomeProviderEvent;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;

public record IncomeProviderBatchResult(Map<String, List<IncomeProviderEvent>> events,
		Map<String, AssetProviderUnavailableException> failures) {
	public IncomeProviderBatchResult {
		events = Map.copyOf(events);
		failures = Map.copyOf(failures);
	}
}
