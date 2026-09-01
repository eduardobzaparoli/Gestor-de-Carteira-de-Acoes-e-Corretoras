package com.bominvestidor.spring.repository.income;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventStatus;
import com.bominvestidor.spring.entity.income.PortfolioIncomeEventEntity;

public interface PortfolioIncomeEventRepository extends JpaRepository<PortfolioIncomeEventEntity, UUID> {
	List<PortfolioIncomeEventEntity> findAllByPortfolio_IdOrderByPaymentDateDescCreatedAtDesc(UUID portfolioId);
	List<PortfolioIncomeEventEntity> findAllByPortfolio_Id(UUID portfolioId);
	List<PortfolioIncomeEventEntity> findAllByPortfolio_IdAndStatus(UUID portfolioId, IncomeEventStatus status);
	Optional<PortfolioIncomeEventEntity> findByIdAndPortfolio_Id(UUID id, UUID portfolioId);
	boolean existsByPortfolio_IdAndSourceAndEventKey(UUID portfolioId, IncomeEventSource source, String eventKey);
}
