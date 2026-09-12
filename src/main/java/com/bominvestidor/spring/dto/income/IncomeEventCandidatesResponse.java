package com.bominvestidor.spring.dto.income;

import java.time.Instant;
import java.util.List;

public record IncomeEventCandidatesResponse(List<IncomeEventCandidateResponse> candidates, Instant updatedAt,
		boolean stale, List<IncomeEventWarningResponse> warnings) {
	public IncomeEventCandidatesResponse {
		candidates = List.copyOf(candidates);
		warnings = List.copyOf(warnings);
	}
}
