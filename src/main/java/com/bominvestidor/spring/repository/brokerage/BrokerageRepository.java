package com.bominvestidor.spring.repository.brokerage;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;

public interface BrokerageRepository extends JpaRepository<BrokerageEntity, UUID> {

	boolean existsByOwner_IdAndCnpj(UUID ownerId, String cnpj);

	boolean existsByOwner_IdAndNicknameKey(UUID ownerId, String nicknameKey);

	List<BrokerageEntity> findAllByOwner_IdOrderByCreatedAtDesc(UUID ownerId);
}
