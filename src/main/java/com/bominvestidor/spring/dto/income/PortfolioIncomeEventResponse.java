package com.bominvestidor.spring.dto.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventStatus;
import com.bominvestidor.spring.domain.income.IncomeEventType;

public record PortfolioIncomeEventResponse(UUID id, String ticker, String assetName, AssetMarket market,
		AssetType assetType, String currency, IncomeEventType type, IncomeEventSource source, IncomeEventStatus status,
		LocalDate eligibilityDate, LocalDate paymentDate, BigDecimal eligibleQuantity, BigDecimal unitAmount,
		BigDecimal expectedAmount, BigDecimal receivedAmount, String adjustmentReason, String notes,
		Instant createdAt, Instant updatedAt) { }
