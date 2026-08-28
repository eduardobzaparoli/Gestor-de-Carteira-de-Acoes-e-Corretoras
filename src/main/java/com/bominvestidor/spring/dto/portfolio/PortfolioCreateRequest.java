package com.bominvestidor.spring.dto.portfolio;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PortfolioCreateRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull UUID brokerageId) {
}
