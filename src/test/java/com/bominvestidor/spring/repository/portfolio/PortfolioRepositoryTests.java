package com.bominvestidor.spring.repository.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.user.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
class PortfolioRepositoryTests {

	private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

	@Autowired private PortfolioRepository portfolioRepository;
	@Autowired private BrokerageRepository brokerageRepository;
	@Autowired private UserRepository userRepository;

	@Test
	void requiresBrokerageAndEnforcesUniqueNamePerOwner() {
		UserEntity owner = saveUser();
		BrokerageEntity brokerage = saveBrokerage(owner);
		portfolioRepository.saveAndFlush(portfolio(owner, brokerage, "Longo prazo", "longo prazo", NOW));

		assertThrows(DataIntegrityViolationException.class, () -> portfolioRepository.saveAndFlush(
				portfolio(owner, brokerage, "LONGO PRAZO", "longo prazo", NOW.plusSeconds(1))));
		assertThrows(DataIntegrityViolationException.class, () -> portfolioRepository.saveAndFlush(
				new PortfolioEntity(UUID.randomUUID(), owner, null, "Sem corretora", "sem corretora", NOW, NOW)));
	}

	@Test
	void listsOwnerPortfoliosFromOldestToNewest() {
		UserEntity owner = saveUser();
		BrokerageEntity brokerage = saveBrokerage(owner);
		portfolioRepository.saveAndFlush(portfolio(owner, brokerage, "Mais nova", "mais nova", NOW.plusSeconds(10)));
		portfolioRepository.saveAndFlush(portfolio(owner, brokerage, "Mais antiga", "mais antiga", NOW));

		assertEquals(java.util.List.of("Mais antiga", "Mais nova"),
				portfolioRepository.findAllByOwner_IdOrderByCreatedAtAscIdAsc(owner.getId()).stream()
						.map(PortfolioEntity::getName).toList());
	}

	@Test
	void databaseConstraintProtectsUpdateAgainstDuplicateNormalizedName() {
		UserEntity owner = saveUser();
		BrokerageEntity brokerage = saveBrokerage(owner);
		portfolioRepository.saveAndFlush(portfolio(owner, brokerage, "Primeira", "primeira", NOW));
		PortfolioEntity second = portfolioRepository.saveAndFlush(
				portfolio(owner, brokerage, "Segunda", "segunda", NOW.plusSeconds(1)));

		second.updateDetails("PRIMEIRA", "primeira", brokerage);

		assertThrows(DataIntegrityViolationException.class, () -> portfolioRepository.saveAndFlush(second));
	}

	private UserEntity saveUser() {
		UUID id = UUID.randomUUID();
		return userRepository.saveAndFlush(new UserEntity(id, "Investidor", id + "@example.com", "hash",
				UserRole.INVESTOR, NOW, NOW));
	}

	private BrokerageEntity saveBrokerage(UserEntity owner) {
		return brokerageRepository.saveAndFlush(new BrokerageEntity(UUID.randomUUID(), owner, "Corretora", "corretora",
				"04252011000110", "Razão Social", "Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS", "04547000",
				"Rua", "Bairro", "1", null, "São Paulo", "SP", NOW, NOW));
	}

	private PortfolioEntity portfolio(UserEntity owner, BrokerageEntity brokerage, String name, String nameKey,
			Instant createdAt) {
		return new PortfolioEntity(UUID.randomUUID(), owner, brokerage, name, nameKey, createdAt, createdAt);
	}
}
