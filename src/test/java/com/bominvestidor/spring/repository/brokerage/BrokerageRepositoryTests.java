package com.bominvestidor.spring.repository.brokerage;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.user.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
class BrokerageRepositoryTests {

	private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");

	@Autowired private BrokerageRepository brokerageRepository;
	@Autowired private UserRepository userRepository;

	@Test
	void allowsSameCnpjForDifferentOwners() {
		UserEntity first = saveUser();
		UserEntity second = saveUser();

		brokerageRepository.saveAndFlush(brokerage(first, "Primeira", "primeira"));
		brokerageRepository.saveAndFlush(brokerage(second, "Primeira", "primeira"));

		org.junit.jupiter.api.Assertions.assertEquals(1,
				brokerageRepository.findAllByOwner_IdOrderByCreatedAtDesc(first.getId()).size());
		org.junit.jupiter.api.Assertions.assertEquals(1,
				brokerageRepository.findAllByOwner_IdOrderByCreatedAtDesc(second.getId()).size());
	}

	@Test
	void rejectsDuplicateCnpjAndNicknameForSameOwner() {
		UserEntity owner = saveUser();
		brokerageRepository.saveAndFlush(brokerage(owner, "Primeira", "primeira"));

		assertThrows(DataIntegrityViolationException.class,
				() -> brokerageRepository.saveAndFlush(brokerage(owner, "Outra", "outra")));
		assertThrows(DataIntegrityViolationException.class,
				() -> brokerageRepository.saveAndFlush(brokerage(owner, "PRIMEIRA", "primeira")));
	}

	private UserEntity saveUser() {
		UUID id = UUID.randomUUID();
		return userRepository.saveAndFlush(new UserEntity(id, "Investidor", id + "@example.com", "hash",
				UserRole.INVESTOR, NOW, NOW));
	}

	private BrokerageEntity brokerage(UserEntity owner, String nickname, String nicknameKey) {
		return new BrokerageEntity(UUID.randomUUID(), owner, nickname, nicknameKey, "04252011000110", "Razão Social",
				"Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS", "04547000", "Rua", "Bairro", "1", null,
				"São Paulo", "SP", NOW, NOW);
	}
}
