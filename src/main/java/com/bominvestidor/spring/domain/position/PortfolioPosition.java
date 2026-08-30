package com.bominvestidor.spring.domain.position;

import java.math.BigDecimal;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

public record PortfolioPosition(String ticker, String assetName, AssetMarket market, AssetType assetType, String currency,
		BigDecimal quantity, BigDecimal averagePrice, BigDecimal custodyCost) { }
