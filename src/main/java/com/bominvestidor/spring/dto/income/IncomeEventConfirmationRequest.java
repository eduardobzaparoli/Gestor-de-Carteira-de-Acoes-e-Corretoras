package com.bominvestidor.spring.dto.income;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IncomeEventConfirmationRequest(@NotNull UUID candidateId,
		@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal receivedAmount,
		@Size(max = 500) String adjustmentReason) { }
