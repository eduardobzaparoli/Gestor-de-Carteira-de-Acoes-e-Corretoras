package com.bominvestidor.spring.repository.portfolio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;

import jakarta.persistence.LockModeType;

public interface PortfolioRepository extends JpaRepository<PortfolioEntity, UUID> {

	boolean existsByOwner_IdAndNameKey(UUID ownerId, String nameKey);

	boolean existsByOwner_IdAndNameKeyAndIdNot(UUID ownerId, String nameKey, UUID id);

	boolean existsByBrokerage_Id(UUID brokerageId);

	@EntityGraph(attributePaths = { "owner", "brokerage" })
	Optional<PortfolioEntity> findByIdAndOwner_Id(UUID id, UUID ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select portfolio from PortfolioEntity portfolio where portfolio.id = :id")
	Optional<PortfolioEntity> findByIdForUpdate(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "owner", "brokerage" })
	List<PortfolioEntity> findAllByOwner_IdOrderByCreatedAtAscIdAsc(UUID ownerId);
}
