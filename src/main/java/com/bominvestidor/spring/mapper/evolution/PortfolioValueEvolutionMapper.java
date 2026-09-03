package com.bominvestidor.spring.mapper.evolution;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.evolution.PortfolioValueEvolutionPoint;
import com.bominvestidor.spring.dto.evolution.PortfolioValueEvolutionPointResponse;

@Component
public class PortfolioValueEvolutionMapper {
	public List<PortfolioValueEvolutionPointResponse> toResponse(List<PortfolioValueEvolutionPoint> values) {
		return values.stream().map(value -> new PortfolioValueEvolutionPointResponse(value.date(), decimal(value.investedValue()), decimal(value.marketValue()))).toList();
	}
	private BigDecimal decimal(BigDecimal value) {
		BigDecimal rounded = value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
		return rounded.scale() < 0 ? rounded.setScale(0) : rounded;
	}
}
