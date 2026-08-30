package com.bominvestidor.spring.repository.transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransactionEntity, UUID> {
	List<PortfolioTransactionEntity> findAllByPortfolio_IdOrderByTransactionDateDescCreatedAtDesc(UUID portfolioId);
	List<PortfolioTransactionEntity> findAllByPortfolio_Id(UUID portfolioId);
	Optional<PortfolioTransactionEntity> findByIdAndPortfolio_Id(UUID id, UUID portfolioId);
	boolean existsByPortfolio_Id(UUID portfolioId);
}
