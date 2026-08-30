package com.bominvestidor.spring.dto.position;

import java.math.BigDecimal;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

public record PortfolioPositionResponse(String ticker, String assetName, AssetMarket market, AssetType assetType,
		String currency, BigDecimal quantity, BigDecimal averagePrice, BigDecimal custodyCost) { }
