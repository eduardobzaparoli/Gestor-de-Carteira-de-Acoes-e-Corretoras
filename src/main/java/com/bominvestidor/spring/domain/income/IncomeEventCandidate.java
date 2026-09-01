package com.bominvestidor.spring.domain.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;

public record IncomeEventCandidate(String eventKey, String ticker, String assetName, AssetMarket market,
		AssetType assetType, String currency, IncomeEventType type, IncomeEventSource source,
		BigDecimal unitAmount, LocalDate eligibilityDate, LocalDate paymentDate, BigDecimal eligibleQuantity,
		BigDecimal expectedAmount, boolean confirmable, boolean alreadyRecorded) { }
