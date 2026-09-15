package com.bominvestidor.spring.dto.evolution;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioValueEvolutionPointResponse(LocalDate date, BigDecimal investedValue, BigDecimal marketValue) { }
