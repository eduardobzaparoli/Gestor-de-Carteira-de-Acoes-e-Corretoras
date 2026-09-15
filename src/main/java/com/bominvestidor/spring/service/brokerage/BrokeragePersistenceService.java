package com.bominvestidor.spring.service.brokerage;

import java.util.List;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.brokerage.Brokerage;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.BrokerageConflictException;
import com.bominvestidor.spring.mapper.brokerage.BrokerageMapper;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.user.UserRepository;

@Service
public class BrokeragePersistenceService {

	private static final String CNPJ_CONSTRAINT = "uk_brokerages_owner_cnpj";
	private static final String NICKNAME_CONSTRAINT = "uk_brokerages_owner_nickname_key";

	private final BrokerageRepository brokerageRepository;
	private final UserRepository userRepository;
	private final BrokerageMapper mapper;

	public BrokeragePersistenceService(BrokerageRepository brokerageRepository, UserRepository userRepository,
			BrokerageMapper mapper) {
		this.brokerageRepository = brokerageRepository;
		this.userRepository = userRepository;
		this.mapper = mapper;
	}

	@Transactional
	public Brokerage save(Brokerage brokerage) {
		if (brokerageRepository.existsByOwner_IdAndCnpj(brokerage.ownerId(), brokerage.cnpj())) {
			throw new BrokerageConflictException("BROKERAGE_CNPJ_ALREADY_REGISTERED", "CNPJ is already registered");
		}
		if (brokerageRepository.existsByOwner_IdAndNicknameKey(brokerage.ownerId(), brokerage.nicknameKey())) {
			throw new BrokerageConflictException("BROKERAGE_NICKNAME_ALREADY_REGISTERED", "Nickname is already registered");
		}
		UserEntity owner = userRepository.findById(brokerage.ownerId())
				.orElseThrow(() -> new IllegalStateException("Authenticated user does not exist"));
		try {
			BrokerageEntity saved = brokerageRepository.saveAndFlush(mapper.toEntity(brokerage, owner));
			return mapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException exception) {
			throw translateConstraint(exception);
		}
	}

	@Transactional(readOnly = true)
	public List<Brokerage> findAllByOwner(UUID ownerId) {
		return brokerageRepository.findAllByOwner_IdOrderByCreatedAtDesc(ownerId).stream().map(mapper::toDomain).toList();
	}

	private RuntimeException translateConstraint(DataIntegrityViolationException exception) {
		String constraint = findConstraintName(exception);
		if (CNPJ_CONSTRAINT.equalsIgnoreCase(constraint)) {
			return new BrokerageConflictException("BROKERAGE_CNPJ_ALREADY_REGISTERED", "CNPJ is already registered");
		}
		if (NICKNAME_CONSTRAINT.equalsIgnoreCase(constraint)) {
			return new BrokerageConflictException("BROKERAGE_NICKNAME_ALREADY_REGISTERED", "Nickname is already registered");
		}
		return exception;
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
