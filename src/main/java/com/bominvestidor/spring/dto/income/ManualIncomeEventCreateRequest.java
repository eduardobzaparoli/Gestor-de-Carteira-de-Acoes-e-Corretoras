package com.bominvestidor.spring.dto.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.income.IncomeEventType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualIncomeEventCreateRequest(
		@NotBlank @Size(max = 32) String ticker, @NotBlank @Size(max = 200) String assetName,
		@NotNull AssetMarket market, @NotNull AssetType assetType, @NotBlank @Size(max = 8) String currency,
		@NotNull IncomeEventType type, @NotNull LocalDate paymentDate,
		@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal receivedAmount,
		LocalDate eligibilityDate, @DecimalMin(value = "0", inclusive = false) BigDecimal eligibleQuantity,
		@DecimalMin(value = "0", inclusive = false) BigDecimal unitAmount, @Size(max = 500) String notes) { }
