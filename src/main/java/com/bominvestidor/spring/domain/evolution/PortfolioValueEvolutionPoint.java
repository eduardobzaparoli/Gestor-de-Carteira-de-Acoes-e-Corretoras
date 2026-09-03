package com.bominvestidor.spring.domain.evolution;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioValueEvolutionPoint(LocalDate date, BigDecimal investedValue, BigDecimal marketValue) {
	public PortfolioValueEvolutionPoint {
		if (date == null || investedValue == null || marketValue == null) throw new IllegalArgumentException("Evolution point is invalid");
	}
}
