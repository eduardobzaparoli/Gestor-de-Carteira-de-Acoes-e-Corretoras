package com.bominvestidor.spring.dto.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;

public record PortfolioTransactionResponse(UUID id, String ticker, String assetName, AssetMarket market, AssetType assetType,
		String currency, TransactionType type, TransactionStatus status, LocalDate transactionDate, BigDecimal quantity,
		BigDecimal unitPrice, BigDecimal costs, Instant createdAt, Instant updatedAt) { }
