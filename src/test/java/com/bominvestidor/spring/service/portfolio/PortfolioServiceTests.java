package com.bominvestidor.spring.service.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bominvestidor.spring.domain.portfolio.Portfolio;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.portfolio.PortfolioCreateRequest;
import com.bominvestidor.spring.dto.portfolio.PortfolioUpdateRequest;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.BrokerageNotFoundException;
import com.bominvestidor.spring.exception.PortfolioNotFoundException;
import com.bominvestidor.spring.mapper.portfolio.PortfolioMapper;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTests {

	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final UUID BROKERAGE_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

	@Mock private UserRepository userRepository;
	@Mock private BrokerageRepository brokerageRepository;
	@Mock private PortfolioPersistenceService persistenceService;

	private PortfolioService service;

	@BeforeEach
	void setUp() {
		service = new PortfolioService(userRepository, brokerageRepository, new PortfolioInputNormalizer(), persistenceService,
				new PortfolioMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
		when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
	}

	@Test
	void createsPortfolioWithTrimmedNameAndExistingOwnerBrokerage() {
		BrokerageEntity brokerage = brokerage(owner());
		when(brokerageRepository.findByIdAndOwner_Id(BROKERAGE_ID, OWNER_ID)).thenReturn(Optional.of(brokerage));
		when(persistenceService.save(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(OWNER_ID, new PortfolioCreateRequest("  Longo prazo  ", BROKERAGE_ID));

		assertEquals("Longo prazo", response.name());
		assertEquals(BROKERAGE_ID, response.brokerage().id());
		ArgumentCaptor<Portfolio> portfolio = ArgumentCaptor.forClass(Portfolio.class);
		verify(persistenceService).save(portfolio.capture(), any(), any());
		assertEquals("longo prazo", portfolio.getValue().nameKey());
	}

	@Test
	void rejectsBrokerageOutsideOwnerWithoutPersisting() {
		when(brokerageRepository.findByIdAndOwner_Id(BROKERAGE_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThrows(BrokerageNotFoundException.class,
				() -> service.create(OWNER_ID, new PortfolioCreateRequest("Longo prazo", BROKERAGE_ID)));

		verify(persistenceService, never()).save(any(), any(), any());
	}

	@Test
	void updatesPortfolioWithNormalizedNameAndOwnedBrokerage() {
		UUID portfolioId = UUID.randomUUID();
		BrokerageEntity brokerage = brokerage(owner());
		PortfolioEntity portfolio = portfolio(brokerage, "Rebalanceamento", "rebalanceamento", NOW);
		when(brokerageRepository.findByIdAndOwner_Id(BROKERAGE_ID, OWNER_ID)).thenReturn(Optional.of(brokerage));
		when(persistenceService.update(any(), any(), any(), any())).thenReturn(portfolio);

		var response = service.update(OWNER_ID, portfolioId,
				new PortfolioUpdateRequest("  Rebalanceamento  ", BROKERAGE_ID));

		assertEquals("Rebalanceamento", response.name());
		ArgumentCaptor<NormalizedPortfolioInput> input = ArgumentCaptor.forClass(NormalizedPortfolioInput.class);
		verify(persistenceService).update(org.mockito.ArgumentMatchers.eq(portfolioId),
				org.mockito.ArgumentMatchers.eq(OWNER_ID), input.capture(), org.mockito.ArgumentMatchers.eq(brokerage));
		assertEquals("rebalanceamento", input.getValue().nameKey());
	}

	@Test
	void listsInPersistenceOrderAndHidesUnknownPortfolio() {
		BrokerageEntity brokerage = brokerage(owner());
		PortfolioEntity older = portfolio(brokerage, "Primeira", "primeira", NOW);
		PortfolioEntity newer = portfolio(brokerage, "Segunda", "segunda", NOW.plusSeconds(1));
		when(persistenceService.findAllEntitiesByOwner(OWNER_ID)).thenReturn(List.of(older, newer));
		when(persistenceService.findEntityByIdAndOwner(any(), any())).thenThrow(new PortfolioNotFoundException());

		assertEquals(List.of("Primeira", "Segunda"), service.findAll(OWNER_ID).stream().map(response -> response.name()).toList());
		assertThrows(PortfolioNotFoundException.class, () -> service.findById(OWNER_ID, UUID.randomUUID()));
	}

	@Test
	void deletesOnlyThePortfolioResolvedForItsOwner() {
		UUID portfolioId = UUID.randomUUID();

		service.delete(OWNER_ID, portfolioId);

		verify(persistenceService).deleteByIdAndOwner(portfolioId, OWNER_ID);
	}

	private UserEntity owner() {
		return new UserEntity(OWNER_ID, "Investidor", "investidor@example.com", "hash", UserRole.INVESTOR, NOW, NOW);
	}

	private BrokerageEntity brokerage(UserEntity owner) {
		return new BrokerageEntity(BROKERAGE_ID, owner, "Corretora", "corretora", "04252011000110", "Razão Social",
				"Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS", "04547000", "Rua", "Bairro", "1", null,
				"São Paulo", "SP", NOW, NOW);
	}

	private PortfolioEntity portfolio(BrokerageEntity brokerage, String name, String nameKey, Instant createdAt) {
		return new PortfolioEntity(UUID.randomUUID(), owner(), brokerage, name, nameKey, createdAt, createdAt);
	}
}
