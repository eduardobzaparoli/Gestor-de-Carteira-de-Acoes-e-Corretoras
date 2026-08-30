package com.bominvestidor.spring.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.dto.portfolio.PortfolioCreateRequest;
import com.bominvestidor.spring.dto.transaction.PortfolioTransactionCreateRequest;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.PortfolioNotFoundException;
import com.bominvestidor.spring.exception.PortfolioConflictException;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategy;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;
import com.bominvestidor.spring.service.asset.AssetSearchService;
import com.bominvestidor.spring.service.portfolio.PortfolioService;
import com.bominvestidor.spring.service.position.PortfolioPositionService;
import com.bominvestidor.spring.service.transaction.PortfolioTransactionService;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "runPostgresTests", matches = "true")
@Import(PostgresPortfolioIntegrationTests.AssetProviderStubConfiguration.class)
class PostgresPortfolioIntegrationTests {

	@Autowired private AuthService authService;
	@Autowired private PortfolioService portfolioService;
	@Autowired private PortfolioRepository portfolioRepository;
	@Autowired private BrokerageRepository brokerageRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private AssetSearchService assetSearchService;
	@Autowired private JdbcTemplate jdbcTemplate;
	@Autowired private PortfolioTransactionService transactionService;
	@Autowired private PortfolioPositionService positionService;

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
			UUID createdPortfolioId = portfolioId;
			assertEquals("Carteira PostgreSQL", portfolioService.findById(userId, createdPortfolioId).name());
			assertEquals(1, portfolioService.findAll(userId).size());

			List<String> assetTablesBefore = assetPersistenceTables();
			List<Long> persistedRowsBefore = persistedRows();
			var assets = assetSearchService.search(userId, portfolioId, "BR", "STOCK", "PETR");
			assertEquals(1, assets.size());
			assertEquals("PETR4", assets.get(0).ticker());
			assertEquals(new BigDecimal("35.10"), assets.get(0).price());
			assertEquals(assetTablesBefore, assetPersistenceTables());
			assertEquals(persistedRowsBefore, persistedRows());

			transactionService.create(userId, portfolioId, new PortfolioTransactionCreateRequest("PETR4", "Petrobras PN",
					AssetMarket.BR, AssetType.STOCK, "BRL", TransactionType.BUY, LocalDate.now(), new BigDecimal("2"),
					new BigDecimal("35.10"), null));
			assertEquals(1, jdbcTemplate.queryForObject(
					"select count(*) from portfolio_transactions where portfolio_id = ?", Integer.class, portfolioId));
			assertEquals(0, new BigDecimal("35.10").compareTo(positionService.findAll(userId, portfolioId).get(0).averagePrice()));
			assertEquals(List.of(), positionPersistenceTables());
			assertThrows(PortfolioConflictException.class, () -> portfolioService.delete(userId, createdPortfolioId));
			deleteTransactions(portfolioId);

			portfolioService.delete(userId, portfolioId);
			UUID deletedPortfolioId = portfolioId;
			assertThrows(PortfolioNotFoundException.class,
					() -> portfolioService.findById(userId, deletedPortfolioId));
			portfolioId = null;
		}
		finally {
			if (portfolioId != null) {
				deleteTransactions(portfolioId);
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

	private void deleteTransactions(UUID portfolioId) {
		jdbcTemplate.update("delete from portfolio_transactions where portfolio_id = ?", portfolioId);
	}

	private List<String> assetPersistenceTables() {
		return jdbcTemplate.queryForList("""
				select table_name
				from information_schema.tables
				where table_schema = 'public'
				  and (lower(table_name) like '%asset%' or lower(table_name) like '%quote%')
				order by table_name
				""", String.class);
	}

	private List<String> positionPersistenceTables() {
		return jdbcTemplate.queryForList("""
				select table_name from information_schema.tables
				where table_schema = 'public' and lower(table_name) like '%position%'
				order by table_name
				""", String.class);
	}

	private List<Long> persistedRows() {
		return List.of(
				jdbcTemplate.queryForObject("select count(*) from users", Long.class),
				jdbcTemplate.queryForObject("select count(*) from brokerages", Long.class),
				jdbcTemplate.queryForObject("select count(*) from portfolios", Long.class));
	}

	private BrokerageEntity saveBrokerage(UserEntity owner) {
		Instant now = Instant.now();
		return brokerageRepository.saveAndFlush(new BrokerageEntity(UUID.randomUUID(), owner, "PostgreSQL", "postgresql",
				"04252011000110", "Razão Social", "Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS", "04547000",
				"Rua", "Bairro", "1", null, "São Paulo", "SP", now, now));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class AssetProviderStubConfiguration {
		@Bean
		@Primary
		AssetSearchStrategyResolver assetSearchStrategyResolver() {
			return new AssetSearchStrategyResolver(List.of(new AssetSearchStrategy() {
				@Override public AssetMarket market() { return AssetMarket.BR; }
				@Override public List<AssetCandidate> findCandidates(AssetType type, String query) {
					return List.of(new AssetCandidate("PETR4", "Petrobras PN", AssetMarket.BR, type, "BRL"));
				}
				@Override public Optional<AssetQuote> findQuote(String ticker) {
					return Optional.of(new AssetQuote(ticker, "BRL", new BigDecimal("35.10")));
				}
			}));
		}
	}
}
