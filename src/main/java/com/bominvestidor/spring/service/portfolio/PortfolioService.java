package com.bominvestidor.spring.service.portfolio;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.portfolio.Portfolio;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.portfolio.PortfolioCreateRequest;
import com.bominvestidor.spring.dto.portfolio.PortfolioResponse;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AuthenticatedUserNotFoundException;
import com.bominvestidor.spring.exception.BrokerageNotFoundException;
import com.bominvestidor.spring.mapper.portfolio.PortfolioMapper;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.user.UserRepository;

@Service
public class PortfolioService {

	private final UserRepository userRepository;
	private final BrokerageRepository brokerageRepository;
	private final PortfolioInputNormalizer normalizer;
	private final PortfolioPersistenceService persistenceService;
	private final PortfolioMapper mapper;
	private final Clock clock;

	public PortfolioService(UserRepository userRepository, BrokerageRepository brokerageRepository,
			PortfolioInputNormalizer normalizer, PortfolioPersistenceService persistenceService, PortfolioMapper mapper,
			Clock clock) {
		this.userRepository = userRepository;
		this.brokerageRepository = brokerageRepository;
		this.normalizer = normalizer;
		this.persistenceService = persistenceService;
		this.mapper = mapper;
		this.clock = clock;
	}

	public PortfolioResponse create(UUID ownerId, PortfolioCreateRequest request) {
		UserEntity owner = requireInvestor(ownerId);
		NormalizedPortfolioInput input = normalizer.normalize(request);
		BrokerageEntity brokerage = brokerageRepository.findByIdAndOwner_Id(input.brokerageId(), ownerId)
				.orElseThrow(BrokerageNotFoundException::new);
		Instant now = clock.instant();
		Portfolio portfolio = new Portfolio(UUID.randomUUID(), ownerId, brokerage.getId(), input.name(), input.nameKey(), now, now);
		return mapper.toResponse(persistenceService.save(portfolio, owner, brokerage), brokerage);
	}

	public List<PortfolioResponse> findAll(UUID ownerId) {
		requireInvestor(ownerId);
		return persistenceService.findAllEntitiesByOwner(ownerId).stream().map(this::toResponse).toList();
	}

	public PortfolioResponse findById(UUID ownerId, UUID id) {
		requireInvestor(ownerId);
		return toResponse(persistenceService.findEntityByIdAndOwner(id, ownerId));
	}

	public void delete(UUID ownerId, UUID id) {
		requireInvestor(ownerId);
		persistenceService.deleteByIdAndOwner(id, ownerId);
	}

	private PortfolioResponse toResponse(PortfolioEntity entity) {
		return mapper.toResponse(mapper.toDomain(entity), entity.getBrokerage());
	}

	private UserEntity requireInvestor(UUID ownerId) {
		UserEntity user = userRepository.findById(ownerId).orElseThrow(AuthenticatedUserNotFoundException::new);
		if (user.getRole() != UserRole.INVESTOR) {
			throw new AccessDeniedException("Portfolios are private to investors");
		}
		return user;
	}
}
