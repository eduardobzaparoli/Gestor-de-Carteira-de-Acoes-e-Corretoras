package com.bominvestidor.spring.service.asset;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.RegisteredAsset;
import com.bominvestidor.spring.domain.asset.SelectedAsset;
import com.bominvestidor.spring.dto.asset.RegisteredAssetCreateRequest;
import com.bominvestidor.spring.dto.asset.RegisteredAssetQuoteResponse;
import com.bominvestidor.spring.dto.asset.RegisteredAssetResponse;
import com.bominvestidor.spring.dto.error.FieldErrorResponse;
import com.bominvestidor.spring.entity.asset.RegisteredAssetEntity;
import com.bominvestidor.spring.exception.InvalidAssetSearchDataException;
import com.bominvestidor.spring.exception.RegisteredAssetConflictException;
import com.bominvestidor.spring.exception.RegisteredAssetNotFoundException;
import com.bominvestidor.spring.mapper.asset.RegisteredAssetMapper;
import com.bominvestidor.spring.repository.asset.RegisteredAssetRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.position.PortfolioPositionService;

@Service
public class RegisteredAssetService {
	private final RegisteredAssetRepository repository;
	private final UserRepository userRepository;
	private final RegisteredAssetMapper mapper;
	private final AssetSelectionCache selectionCache;
	private final FreshAssetQuoteService freshQuoteService;
	private final PortfolioRepository portfolioRepository;
	private final PortfolioPositionService positionService;
	private final Clock clock;

	public RegisteredAssetService(RegisteredAssetRepository repository, UserRepository userRepository,
			RegisteredAssetMapper mapper, AssetSelectionCache selectionCache, FreshAssetQuoteService freshQuoteService,
			PortfolioRepository portfolioRepository, PortfolioPositionService positionService, Clock clock) {
		this.repository = repository;
		this.userRepository = userRepository;
		this.mapper = mapper;
		this.selectionCache = selectionCache;
		this.freshQuoteService = freshQuoteService;
		this.portfolioRepository = portfolioRepository;
		this.positionService = positionService;
		this.clock = clock;
	}

	@Transactional
	public RegisteredAssetResponse register(UUID ownerId, RegisteredAssetCreateRequest request) {
		SelectedAsset selected = selectionCache.find(request.assetSelectionId(), ownerId)
				.orElseThrow(() -> conflict("ASSET_SELECTION_EXPIRED", "Asset selection is expired"));
		String ticker = selected.ticker().trim().toUpperCase(Locale.ROOT);
		if (repository.existsByOwner_IdAndMarketAndTicker(ownerId, selected.market(), ticker)) {
			throw alreadyRegistered();
		}
		AssetQuote quote = freshQuoteService.fetch(selected.market(), ticker);
		Instant now = clock.instant();
		RegisteredAsset asset = new RegisteredAsset(UUID.randomUUID(), ownerId, ticker, selected.name().trim(),
				selected.market(), selected.assetType(), quote.currency().trim().toUpperCase(Locale.ROOT), quote.price(), now,
				now, now);
		try {
			RegisteredAssetEntity saved = repository.saveAndFlush(
					mapper.toEntity(asset, userRepository.getReferenceById(ownerId)));
			selectionCache.remove(request.assetSelectionId());
			return mapper.toResponse(saved);
		} catch (DataIntegrityViolationException exception) {
			throw alreadyRegistered();
		}
	}

	@Transactional(readOnly = true)
	public List<RegisteredAssetResponse> findAll(UUID ownerId, String marketValue) {
		List<RegisteredAssetEntity> assets;
		if (marketValue == null || marketValue.isBlank()) {
			assets = repository.findAllByOwner_IdOrderByMarketAscTickerAscIdAsc(ownerId);
		} else {
			assets = repository.findAllByOwner_IdAndMarketOrderByTickerAscIdAsc(ownerId, parseMarket(marketValue));
		}
		return assets.stream().map(mapper::toResponse).toList();
	}

	@Transactional
	public RegisteredAssetResponse refreshQuote(UUID ownerId, UUID assetId) {
		RegisteredAssetEntity asset = requireOwnedEntity(ownerId, assetId);
		AssetQuote quote = freshQuoteService.fetch(asset.getMarket(), asset.getTicker());
		asset.updateQuote(quote.price(), clock.instant());
		return mapper.toResponse(asset);
	}

	@Transactional(readOnly = true)
	public RegisteredAssetQuoteResponse freshQuote(UUID ownerId, UUID assetId) {
		RegisteredAssetEntity asset = requireOwnedEntity(ownerId, assetId);
		AssetQuote quote = freshQuoteService.fetch(asset.getMarket(), asset.getTicker());
		return new RegisteredAssetQuoteResponse(asset.getId(), asset.getTicker(), asset.getMarket(), quote.currency(),
				quote.price(), clock.instant());
	}

	@Transactional
	public void delete(UUID ownerId, UUID assetId) {
		RegisteredAssetEntity asset = requireOwnedEntity(ownerId, assetId);
		validateWithoutPositivePosition(ownerId, List.of(asset));
		repository.delete(asset);
	}

	@Transactional
	public void deleteAll(UUID ownerId) {
		List<RegisteredAssetEntity> assets = repository.findAllByOwner_IdOrderByMarketAscTickerAscIdAsc(ownerId);
		validateWithoutPositivePosition(ownerId, assets);
		repository.deleteAll(assets);
	}

	@Transactional(readOnly = true)
	public RegisteredAssetEntity requireOwnedEntity(UUID ownerId, UUID assetId) {
		return repository.findByIdAndOwner_Id(assetId, ownerId).orElseThrow(RegisteredAssetNotFoundException::new);
	}

	private AssetMarket parseMarket(String value) {
		try {
			return AssetMarket.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new InvalidAssetSearchDataException(
					List.of(new FieldErrorResponse("market", "Invalid value")));
		}
	}

	private RegisteredAssetConflictException alreadyRegistered() {
		return conflict("ASSET_ALREADY_REGISTERED", "Asset is already registered");
	}

	private void validateWithoutPositivePosition(UUID ownerId, List<RegisteredAssetEntity> assets) {
		if (assets.isEmpty()) return;
		boolean hasPositivePosition = portfolioRepository.findAllByOwner_IdOrderByCreatedAtAscIdAsc(ownerId).stream()
				.flatMap(portfolio -> positionService.findAll(ownerId, portfolio.getId()).stream())
				.filter(position -> position.quantity().signum() > 0)
				.anyMatch(position -> assets.stream().anyMatch(asset ->
						asset.getMarket() == position.market() && asset.getTicker().equalsIgnoreCase(position.ticker())));
		if (hasPositivePosition) {
			throw conflict("REGISTERED_ASSET_HAS_POSITION",
					"Asset cannot be deleted while it has a positive portfolio position");
		}
	}

	private RegisteredAssetConflictException conflict(String code, String message) {
		return new RegisteredAssetConflictException(code, message);
	}
}
