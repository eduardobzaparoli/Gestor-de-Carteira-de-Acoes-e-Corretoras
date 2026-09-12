package com.bominvestidor.spring.service.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.dto.position.PortfolioPositionResponse;
import com.bominvestidor.spring.entity.asset.RegisteredAssetEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.exception.RegisteredAssetConflictException;
import com.bominvestidor.spring.mapper.asset.RegisteredAssetMapper;
import com.bominvestidor.spring.repository.asset.RegisteredAssetRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.position.PortfolioPositionService;

class RegisteredAssetDeletionServiceTests {
	private final RegisteredAssetRepository repository = mock(RegisteredAssetRepository.class);
	private final PortfolioRepository portfolioRepository = mock(PortfolioRepository.class);
	private final PortfolioPositionService positionService = mock(PortfolioPositionService.class);
	private RegisteredAssetService service;

	@BeforeEach
	void setUp() {
		service = new RegisteredAssetService(repository, mock(UserRepository.class), mock(RegisteredAssetMapper.class),
				mock(AssetSelectionCache.class), mock(FreshAssetQuoteService.class), portfolioRepository, positionService,
				Clock.fixed(Instant.parse("2026-09-12T12:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void deletesOwnedAssetWhenNoPortfolioHasPositivePosition() {
		UUID ownerId = UUID.randomUUID();
		RegisteredAssetEntity asset = asset("PETR4", AssetMarket.BR);
		when(repository.findByIdAndOwner_Id(asset.getId(), ownerId)).thenReturn(Optional.of(asset));
		when(portfolioRepository.findAllByOwner_IdOrderByCreatedAtAscIdAsc(ownerId)).thenReturn(List.of());

		service.delete(ownerId, asset.getId());

		verify(repository).delete(asset);
	}

	@Test
	void blocksAtomicBulkDeletionWhenAnyAssetHasPositivePosition() {
		UUID ownerId = UUID.randomUUID();
		RegisteredAssetEntity brAsset = asset("PETR4", AssetMarket.BR);
		RegisteredAssetEntity usAsset = asset("MSFT", AssetMarket.US);
		PortfolioEntity portfolio = mock(PortfolioEntity.class);
		UUID portfolioId = UUID.randomUUID();
		when(portfolio.getId()).thenReturn(portfolioId);
		when(repository.findAllByOwner_IdOrderByMarketAscTickerAscIdAsc(ownerId))
				.thenReturn(List.of(brAsset, usAsset));
		when(portfolioRepository.findAllByOwner_IdOrderByCreatedAtAscIdAsc(ownerId)).thenReturn(List.of(portfolio));
		when(positionService.findAll(ownerId, portfolioId)).thenReturn(List.of(
				new PortfolioPositionResponse("MSFT", "Microsoft", AssetMarket.US, AssetType.STOCK, "USD",
						new BigDecimal("2"), new BigDecimal("300"), BigDecimal.ZERO)));

		RegisteredAssetConflictException error = assertThrows(RegisteredAssetConflictException.class,
				() -> service.deleteAll(ownerId));

		assertEquals("REGISTERED_ASSET_HAS_POSITION", error.getCode());
		verify(repository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
	}

	private RegisteredAssetEntity asset(String ticker, AssetMarket market) {
		Instant now = Instant.parse("2026-09-12T12:00:00Z");
		return new RegisteredAssetEntity(UUID.randomUUID(), null, ticker, ticker, market, AssetType.STOCK,
				market == AssetMarket.BR ? "BRL" : "USD", BigDecimal.TEN, now, now, now);
	}
}
