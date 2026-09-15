package com.bominvestidor.spring.mapper.brokerage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.brokerage.Address;
import com.bominvestidor.spring.domain.brokerage.Brokerage;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.user.UserEntity;

class BrokerageMapperTests {

	private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");

	@Test
	void mapsDomainEntityAndPublicResponseWithoutChangingOwnershipData() {
		UUID ownerId = UUID.randomUUID();
		UserEntity owner = new UserEntity(ownerId, "Investidor", "investidor@example.com", "hash", UserRole.INVESTOR,
				NOW, NOW);
		Brokerage source = new Brokerage(UUID.randomUUID(), ownerId, "Minha Corretora", "minha corretora",
				"04252011000110", "Razão Social", "Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS",
				new Address("04547000", "Rua", "Bairro", "42", "Sala 1", "São Paulo", "SP"), NOW, NOW);
		BrokerageMapper mapper = new BrokerageMapper();

		BrokerageEntity entity = mapper.toEntity(source, owner);
		Brokerage restored = mapper.toDomain(entity);
		var response = mapper.toResponse(restored);

		assertEquals(source, restored);
		assertEquals(source.id(), response.id());
		assertEquals(source.nickname(), response.nickname());
		assertEquals(source.address().city(), response.address().city());
	}
}
