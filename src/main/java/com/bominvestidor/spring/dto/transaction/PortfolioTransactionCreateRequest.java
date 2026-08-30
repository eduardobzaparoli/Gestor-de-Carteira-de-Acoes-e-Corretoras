package com.bominvestidor.spring.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PortfolioTransactionCreateRequest(
	@NotBlank @Size(max = 32) String ticker, @NotBlank @Size(max = 200) String assetName,
	@NotNull AssetMarket market, @NotNull AssetType assetType, @NotBlank @Size(max = 8) String currency,
	@NotNull TransactionType type, @NotNull LocalDate transactionDate,
	@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
	@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal unitPrice,
	@DecimalMin(value = "0") BigDecimal costs) { }
