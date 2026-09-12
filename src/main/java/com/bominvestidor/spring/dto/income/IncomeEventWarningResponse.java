package com.bominvestidor.spring.dto.income;

import com.bominvestidor.spring.domain.asset.AssetMarket;

public record IncomeEventWarningResponse(String ticker, AssetMarket market, String code) { }
