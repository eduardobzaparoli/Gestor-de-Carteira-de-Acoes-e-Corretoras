package com.bominvestidor.spring.dto.income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventType;

public record IncomeEventCandidateResponse(UUID candidateId, String ticker, String assetName, AssetMarket market,
		AssetType assetType, String currency, IncomeEventType type, IncomeEventSource source, BigDecimal unitAmount,
		LocalDate eligibilityDate, LocalDate paymentDate, BigDecimal eligibleQuantity, BigDecimal expectedAmount,
		boolean confirmable, boolean alreadyRecorded) { }
