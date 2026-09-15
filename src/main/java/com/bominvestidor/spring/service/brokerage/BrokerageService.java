package com.bominvestidor.spring.service.brokerage;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.brokerage.Address;
import com.bominvestidor.spring.domain.brokerage.Brokerage;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.brokerage.BrokerageCreateRequest;
import com.bominvestidor.spring.dto.brokerage.BrokerageResponse;
import com.bominvestidor.spring.dto.brokerage.CepLookupResponse;
import com.bominvestidor.spring.dto.brokerage.CnpjLookupResponse;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AuthenticatedUserNotFoundException;
import com.bominvestidor.spring.exception.BrokerageConflictException;
import com.bominvestidor.spring.exception.BrokerageNotFoundException;
import com.bominvestidor.spring.exception.BrokerageProviderUnavailableException;
import com.bominvestidor.spring.exception.BrokerageRuleException;
import com.bominvestidor.spring.exception.CepNotFoundException;
import com.bominvestidor.spring.integration.address.AddressLookupData;
import com.bominvestidor.spring.integration.address.AddressLookupStrategy;
import com.bominvestidor.spring.integration.address.AddressProviderUnavailableException;
import com.bominvestidor.spring.integration.cnpj.CnpjLookupStrategy;
import com.bominvestidor.spring.integration.cnpj.CnpjProviderUnavailableException;
import com.bominvestidor.spring.integration.cnpj.CnpjRegistrationData;
import com.bominvestidor.spring.integration.cvm.CvmParticipantData;
import com.bominvestidor.spring.integration.cvm.CvmParticipantStrategy;
import com.bominvestidor.spring.integration.cvm.CvmProviderUnavailableException;
import com.bominvestidor.spring.mapper.brokerage.BrokerageMapper;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;

@Service
public class BrokerageService {

	private final UserRepository userRepository;
	private final BrokerageRepository brokerageRepository;
	private final CnpjLookupStrategy cnpjLookup;
	private final AddressLookupStrategy addressLookup;
	private final CvmParticipantStrategy cvmParticipantLookup;
	private final BrokerageInputNormalizer normalizer;
	private final BrokeragePersistenceService persistenceService;
	private final BrokerageMapper mapper;
	private final BrokerageIntegrationProperties properties;
	private final Clock clock;
	private final PortfolioRepository portfolioRepository;

	public BrokerageService(UserRepository userRepository, BrokerageRepository brokerageRepository,
			CnpjLookupStrategy cnpjLookup, AddressLookupStrategy addressLookup,
			CvmParticipantStrategy cvmParticipantLookup, BrokerageInputNormalizer normalizer,
			BrokeragePersistenceService persistenceService, BrokerageMapper mapper,
			BrokerageIntegrationProperties properties, Clock clock, PortfolioRepository portfolioRepository) {
		this.userRepository = userRepository;
		this.brokerageRepository = brokerageRepository;
		this.cnpjLookup = cnpjLookup;
		this.addressLookup = addressLookup;
		this.cvmParticipantLookup = cvmParticipantLookup;
		this.normalizer = normalizer;
		this.persistenceService = persistenceService;
		this.mapper = mapper;
		this.properties = properties;
		this.clock = clock;
		this.portfolioRepository = portfolioRepository;
	}

	public CnpjLookupResponse lookupCnpj(UUID ownerId, String rawCnpj) {
		requireInvestor(ownerId);
		String cnpj = normalizer.normalizeCnpj(rawCnpj);
		try {
			CnpjRegistrationData data = cnpjLookup.findByCnpj(cnpj)
					.orElseThrow(() -> new BrokerageRuleException("CNPJ_NOT_FOUND", "CNPJ was not found"));
			return new CnpjLookupResponse(data.cnpj(), data.legalName(), data.tradeName());
		}
		catch (CnpjProviderUnavailableException exception) {
			throw new BrokerageProviderUnavailableException("CNPJ_PROVIDER_UNAVAILABLE", "CNPJ provider is unavailable");
		}
	}

	public CepLookupResponse lookupCep(UUID ownerId, String rawCep) {
		requireInvestor(ownerId);
		String cep = normalizer.normalizeCep(rawCep);
		try {
			AddressLookupData address = addressLookup.findByCep(cep).orElseThrow(CepNotFoundException::new);
			return new CepLookupResponse(address.cep(), address.street(), address.neighborhood(), address.city(), address.state());
		}
		catch (AddressProviderUnavailableException exception) {
			throw new BrokerageProviderUnavailableException("ADDRESS_PROVIDER_UNAVAILABLE", "Address provider is unavailable");
		}
	}

	public BrokerageResponse register(UUID ownerId, BrokerageCreateRequest request) {
		UserEntity owner = requireInvestor(ownerId);
		NormalizedBrokerageInput input = normalizer.normalize(request);
		if (brokerageRepository.existsByOwner_IdAndCnpj(ownerId, input.cnpj())) {
			throw new com.bominvestidor.spring.exception.BrokerageConflictException(
					"BROKERAGE_CNPJ_ALREADY_REGISTERED", "CNPJ is already registered");
		}
		if (brokerageRepository.existsByOwner_IdAndNicknameKey(ownerId, input.nicknameKey())) {
			throw new com.bominvestidor.spring.exception.BrokerageConflictException(
					"BROKERAGE_NICKNAME_ALREADY_REGISTERED", "Nickname is already registered");
		}

		CnpjRegistrationData cnpjData;
		try {
			cnpjData = cnpjLookup.findByCnpj(input.cnpj()).orElseThrow(
					() -> new BrokerageRuleException("CNPJ_NOT_FOUND", "CNPJ was not found"));
		}
		catch (CnpjProviderUnavailableException exception) {
			throw new BrokerageProviderUnavailableException("CNPJ_PROVIDER_UNAVAILABLE", "CNPJ provider is unavailable");
		}

		CvmParticipantData cvmData;
		try {
			cvmData = cvmParticipantLookup.findByCnpj(input.cnpj()).orElseThrow(
					() -> new BrokerageRuleException("CNPJ_NOT_ACTIVE_AT_CVM", "CNPJ is not an active CVM participant"));
		}
		catch (CvmProviderUnavailableException exception) {
			throw new BrokerageProviderUnavailableException("CVM_PROVIDER_UNAVAILABLE", "CVM provider is unavailable");
		}
		if (!properties.getCvmActiveStatus().equalsIgnoreCase(cvmData.status())) {
			throw new BrokerageRuleException("CNPJ_NOT_ACTIVE_AT_CVM", "CNPJ is not an active CVM participant");
		}
		if (!matchesOfficialCep(input.cep(), cnpjData.postalCode())) {
			throw new BrokerageRuleException("CEP_CNPJ_MISMATCH", "CEP does not belong to the CNPJ");
		}

		AddressLookupData address;
		try {
			address = addressLookup.findByCep(input.cep()).orElseThrow(
					() -> new BrokerageRuleException("CEP_NOT_FOUND", "CEP was not found"));
		}
		catch (AddressProviderUnavailableException exception) {
			throw new BrokerageProviderUnavailableException("ADDRESS_PROVIDER_UNAVAILABLE", "Address provider is unavailable");
		}
		Instant now = clock.instant();
		Brokerage brokerage = new Brokerage(UUID.randomUUID(), ownerId, input.nickname(), input.nicknameKey(), input.cnpj(),
				cnpjData.legalName(), cnpjData.tradeName(), cvmData.status(), cvmData.category(),
				new Address(input.cep(), input.street(), input.neighborhood(), input.number(), input.complement(),
						address.city(), address.state()), now, now);
		return mapper.toResponse(persistenceService.save(brokerage));
	}

	public List<BrokerageResponse> findAll(UUID ownerId) {
		requireInvestor(ownerId);
		return persistenceService.findAllByOwner(ownerId).stream().map(mapper::toResponse).toList();
	}

	@Transactional
	public void delete(UUID ownerId, UUID brokerageId) {
		requireInvestor(ownerId);
		var brokerage = brokerageRepository.findByIdAndOwner_Id(brokerageId, ownerId)
				.orElseThrow(BrokerageNotFoundException::new);
		if (portfolioRepository.existsByBrokerage_Id(brokerageId)) {
			throw new BrokerageConflictException(
					"BROKERAGE_HAS_PORTFOLIOS", "Brokerage is linked to a portfolio");
		}
		try {
			brokerageRepository.delete(brokerage);
			brokerageRepository.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw new BrokerageConflictException(
					"BROKERAGE_HAS_PORTFOLIOS", "Brokerage is linked to a portfolio");
		}
	}

	private UserEntity requireInvestor(UUID ownerId) {
		UserEntity user = userRepository.findById(ownerId).orElseThrow(AuthenticatedUserNotFoundException::new);
		if (user.getRole() != UserRole.INVESTOR) {
			throw new AccessDeniedException("Brokerages are private to investors");
		}
		return user;
	}

	private boolean matchesOfficialCep(String requestedCep, String officialCep) {
		if (officialCep == null || officialCep.isBlank()) {
			return false;
		}
		try {
			return requestedCep.equals(normalizer.normalizeCep(officialCep));
		}
		catch (RuntimeException exception) {
			return false;
		}
	}
}
