package com.bominvestidor.spring.repository.portfolio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, UUID> {

	boolean existsByOwner_IdAndNameKey(UUID ownerId, String nameKey);

	@EntityGraph(attributePaths = { "owner", "brokerage" })
	Optional<PortfolioEntity> findByIdAndOwner_Id(UUID id, UUID ownerId);

	@EntityGraph(attributePaths = { "owner", "brokerage" })
	List<PortfolioEntity> findAllByOwner_IdOrderByCreatedAtAscIdAsc(UUID ownerId);
}
