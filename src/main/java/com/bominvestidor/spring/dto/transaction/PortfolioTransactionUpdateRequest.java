package com.bominvestidor.spring.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.bominvestidor.spring.domain.transaction.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record PortfolioTransactionUpdateRequest(
		@NotNull TransactionType type,
		@NotNull LocalDate transactionDate,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8) BigDecimal quantity,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 11, fraction = 8) BigDecimal unitPrice,
		@DecimalMin(value = "0") @Digits(integer = 11, fraction = 8) BigDecimal costs) { }
