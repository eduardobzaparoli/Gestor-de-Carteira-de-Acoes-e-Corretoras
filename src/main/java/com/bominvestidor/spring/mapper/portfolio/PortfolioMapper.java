package com.bominvestidor.spring.mapper.portfolio;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.portfolio.Portfolio;
import com.bominvestidor.spring.dto.portfolio.BrokerageSummaryResponse;
import com.bominvestidor.spring.dto.portfolio.PortfolioResponse;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;

@Component
public class PortfolioMapper {

	public Portfolio toDomain(PortfolioEntity entity) {
		return new Portfolio(entity.getId(), entity.getOwner().getId(), entity.getBrokerage().getId(), entity.getName(),
				entity.getNameKey(), entity.getCreatedAt(), entity.getUpdatedAt());
	}

	public PortfolioEntity toEntity(Portfolio portfolio, UserEntity owner, BrokerageEntity brokerage) {
		return new PortfolioEntity(portfolio.id(), owner, brokerage, portfolio.name(), portfolio.nameKey(),
				portfolio.createdAt(), portfolio.updatedAt());
	}

	public PortfolioResponse toResponse(Portfolio portfolio, BrokerageEntity brokerage) {
		return new PortfolioResponse(portfolio.id(), portfolio.name(),
				new BrokerageSummaryResponse(brokerage.getId(), brokerage.getNickname(), brokerage.getCnpj(),
						brokerage.getLegalName()),
				portfolio.createdAt(), portfolio.updatedAt());
	}
}
