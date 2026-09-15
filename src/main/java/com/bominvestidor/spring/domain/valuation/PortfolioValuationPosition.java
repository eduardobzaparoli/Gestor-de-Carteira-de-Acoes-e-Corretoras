package com.bominvestidor.spring.domain.valuation;

import java.math.BigDecimal;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

public record PortfolioValuationPosition(String ticker, String assetName, AssetMarket market, AssetType assetType,
		String currency, BigDecimal quantity, BigDecimal averagePrice, BigDecimal custodyCost, BigDecimal currentPrice,
		BigDecimal marketValue, BigDecimal unrealizedGain, BigDecimal returnPercentage, BigDecimal allocationPercentage) { }
