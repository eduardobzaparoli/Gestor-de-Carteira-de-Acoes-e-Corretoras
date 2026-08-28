package com.bominvestidor.spring.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.dto.portfolio.PortfolioCreateRequest;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.PortfolioNotFoundException;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "runPostgresTests", matches = "true")
class PostgresPortfolioIntegrationTests {

	@Autowired private AuthService authService;
	@Autowired private PortfolioService portfolioService;
	@Autowired private PortfolioRepository portfolioRepository;
	@Autowired private BrokerageRepository brokerageRepository;
	@Autowired private UserRepository userRepository;

	@Test
	void initializesSchemaAndSupportsPortfolioLifecycle() {
		String email = "postgres-portfolio-" + UUID.randomUUID() + "@example.com";
		UserEntity user = null;
		BrokerageEntity brokerage = null;
		UUID portfolioId = null;

		try {
			authService.register(new RegisterRequest("Postgres Portfolio", email, "password123"));
			user = userRepository.findByEmail(email).orElseThrow();
			brokerage = saveBrokerage(user);
			UUID userId = user.getId();

			var created = portfolioService.create(userId, new PortfolioCreateRequest("Carteira PostgreSQL", brokerage.getId()));
			portfolioId = created.id();
			assertEquals("Carteira PostgreSQL", portfolioService.findById(userId, portfolioId).name());
			assertEquals(1, portfolioService.findAll(userId).size());

			portfolioService.delete(userId, portfolioId);
			UUID deletedPortfolioId = portfolioId;
			assertThrows(PortfolioNotFoundException.class,
					() -> portfolioService.findById(userId, deletedPortfolioId));
		}
		finally {
			if (portfolioId != null) {
				portfolioRepository.deleteById(portfolioId);
			}
			if (brokerage != null) {
				brokerageRepository.deleteById(brokerage.getId());
			}
			if (user != null) {
				userRepository.deleteById(user.getId());
			}
		}
	}

	private BrokerageEntity saveBrokerage(UserEntity owner) {
		Instant now = Instant.now();
		return brokerageRepository.saveAndFlush(new BrokerageEntity(UUID.randomUUID(), owner, "PostgreSQL", "postgresql",
				"04252011000110", "Razão Social", "Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS", "04547000",
				"Rua", "Bairro", "1", null, "São Paulo", "SP", now, now));
	}
}
