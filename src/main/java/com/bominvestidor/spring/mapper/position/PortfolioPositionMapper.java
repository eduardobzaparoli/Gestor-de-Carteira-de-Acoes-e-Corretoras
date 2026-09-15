package com.bominvestidor.spring.mapper.position;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.position.PortfolioPosition;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;

@Component
public class PortfolioPositionMapper {
	public PortfolioPositionResponse toResponse(PortfolioPosition position) {
		return new PortfolioPositionResponse(position.ticker(), position.assetName(), position.market(), position.assetType(),
				position.currency(), position.quantity(), publicAveragePrice(position.averagePrice()), position.custodyCost());
	}

	private BigDecimal publicAveragePrice(BigDecimal value) {
		BigDecimal rounded = value.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
		return rounded.scale() < 0 ? rounded.setScale(0) : rounded;
	}
}
