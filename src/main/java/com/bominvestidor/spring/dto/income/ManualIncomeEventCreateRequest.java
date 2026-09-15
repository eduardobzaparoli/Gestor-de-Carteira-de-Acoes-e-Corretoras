package com.bominvestidor.spring.dto.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bominvestidor.spring.domain.income.IncomeEventType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualIncomeEventCreateRequest(
		@NotBlank @Size(max = 32) String ticker,
		@NotNull IncomeEventType type, @NotNull LocalDate paymentDate,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8) BigDecimal receivedAmount,
		LocalDate eligibilityDate, @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8) BigDecimal eligibleQuantity,
		@DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8) BigDecimal unitAmount, @Size(max = 500) String notes) { }
