package com.bominvestidor.spring.service.portfolio;

import java.util.List;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.portfolio.Portfolio;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.PortfolioConflictException;
import com.bominvestidor.spring.exception.PortfolioNotFoundException;
import com.bominvestidor.spring.mapper.portfolio.PortfolioMapper;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;

@Service
public class PortfolioPersistenceService {

	private static final String NAME_CONSTRAINT = "uk_portfolios_owner_name_key";

	private final PortfolioRepository portfolioRepository;
	private final PortfolioMapper mapper;

	public PortfolioPersistenceService(PortfolioRepository portfolioRepository, PortfolioMapper mapper) {
		this.portfolioRepository = portfolioRepository;
		this.mapper = mapper;
	}

	@Transactional
	public Portfolio save(Portfolio portfolio, UserEntity owner, BrokerageEntity brokerage) {
		if (portfolioRepository.existsByOwner_IdAndNameKey(portfolio.ownerId(), portfolio.nameKey())) {
			throw duplicateName();
		}
		try {
			return mapper.toDomain(portfolioRepository.saveAndFlush(mapper.toEntity(portfolio, owner, brokerage)));
		}
		catch (DataIntegrityViolationException exception) {
			if (NAME_CONSTRAINT.equalsIgnoreCase(findConstraintName(exception))) {
				throw duplicateName();
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public List<PortfolioEntity> findAllEntitiesByOwner(UUID ownerId) {
		return portfolioRepository.findAllByOwner_IdOrderByCreatedAtAscIdAsc(ownerId);
	}

	@Transactional(readOnly = true)
	public PortfolioEntity findEntityByIdAndOwner(UUID id, UUID ownerId) {
		return portfolioRepository.findByIdAndOwner_Id(id, ownerId).orElseThrow(PortfolioNotFoundException::new);
	}

	@Transactional
	public void deleteByIdAndOwner(UUID id, UUID ownerId) {
		portfolioRepository.delete(findEntityByIdAndOwner(id, ownerId));
	}

	private PortfolioConflictException duplicateName() {
		return new PortfolioConflictException("PORTFOLIO_NAME_ALREADY_REGISTERED", "Portfolio name is already registered");
	}

	private String findConstraintName(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof ConstraintViolationException violation) {
				return violation.getConstraintName();
			}
			current = current.getCause();
		}
		return null;
	}
}
