package com.bominvestidor.spring.dto.portfolio;

import java.time.Instant;
import java.util.UUID;

public record PortfolioResponse(UUID id, String name, BrokerageSummaryResponse brokerage,
		Instant createdAt, Instant updatedAt) {
}
