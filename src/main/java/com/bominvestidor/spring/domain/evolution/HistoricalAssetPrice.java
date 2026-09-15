package com.bominvestidor.spring.domain.evolution;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HistoricalAssetPrice(LocalDate date, BigDecimal close) {
	public HistoricalAssetPrice {
		if (date == null || close == null || close.signum() <= 0) {
			throw new IllegalArgumentException("Historical asset price is invalid");
		}
	}
}
