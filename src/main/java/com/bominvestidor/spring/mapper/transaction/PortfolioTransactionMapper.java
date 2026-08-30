package com.bominvestidor.spring.mapper.transaction;

import org.springframework.stereotype.Component;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionResponse;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

@Component
public class PortfolioTransactionMapper {
	public PortfolioTransactionResponse toResponse(PortfolioTransactionEntity entity) {
		return new PortfolioTransactionResponse(entity.getId(), entity.getTicker(), entity.getAssetName(), entity.getMarket(),
				entity.getAssetType(), entity.getCurrency(), entity.getType(), entity.getStatus(), entity.getTransactionDate(),
				entity.getQuantity(), entity.getUnitPrice(), entity.getCosts(), entity.getCreatedAt(), entity.getUpdatedAt());
	}
}
