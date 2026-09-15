package com.bominvestidor.spring.service.brokerage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.brokerage.Address;
import com.bominvestidor.spring.domain.brokerage.Brokerage;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.brokerage.BrokerageCreateRequest;
import com.bominvestidor.spring.exception.BrokerageProviderUnavailableException;
import com.bominvestidor.spring.exception.BrokerageRuleException;
import com.bominvestidor.spring.exception.BrokerageConflictException;
import com.bominvestidor.spring.exception.CepNotFoundException;
import com.bominvestidor.spring.integration.address.AddressProviderUnavailableException;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.integration.address.AddressLookupData;
import com.bominvestidor.spring.integration.address.AddressLookupStrategy;
import com.bominvestidor.spring.integration.cnpj.CnpjLookupStrategy;
import com.bominvestidor.spring.integration.cnpj.CnpjRegistrationData;
import com.bominvestidor.spring.integration.cvm.CvmParticipantData;
import com.bominvestidor.spring.integration.cvm.CvmParticipantStrategy;
import com.bominvestidor.spring.mapper.brokerage.BrokerageMapper;
import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class BrokerageServiceTests {

	private static final UUID OWNER_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-08-26T12:00:00Z");

	@Mock private UserRepository userRepository;
	@Mock private BrokerageRepository brokerageRepository;
	@Mock private CnpjLookupStrategy cnpjLookup;
	@Mock private AddressLookupStrategy addressLookup;
	@Mock private CvmParticipantStrategy cvmLookup;
	@Mock private BrokeragePersistenceService persistenceService;
	@Mock private PortfolioRepository portfolioRepository;

	private BrokerageService service;
	private final BrokerageMapper mapper = new BrokerageMapper();
	private final BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();

	@BeforeEach
	void setUp() {
		service = new BrokerageService(userRepository, brokerageRepository, cnpjLookup, addressLookup, cvmLookup,
				new BrokerageInputNormalizer(), persistenceService, mapper, properties, Clock.fixed(NOW, ZoneOffset.UTC),
				portfolioRepository);
		when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user()));
		lenient().when(brokerageRepository.existsByOwner_IdAndCnpj(any(), any())).thenReturn(false);
		lenient().when(brokerageRepository.existsByOwner_IdAndNicknameKey(any(), any())).thenReturn(false);
	}

	@Test
	void validatesAllSourcesAndPersistsOnlyOfficialCityAndState() {
		when(cnpjLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CnpjRegistrationData("04252011000110", "Razão Social", "Nome", "04547000")));
		when(cvmLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CvmParticipantData("04252011000110", "CORRETORAS", "EM FUNCIONAMENTO NORMAL")));
		when(addressLookup.findByCep("04547000"))
				.thenReturn(Optional.of(new AddressLookupData("04547000", "Rua da ViaCEP", "Bairro da ViaCEP", "São Paulo", "SP")));
		when(persistenceService.save(any(Brokerage.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.register(OWNER_ID, validRequest());

		assertEquals("Rua manual", response.address().street());
		assertEquals("São Paulo", response.address().city());
		assertEquals("SP", response.address().state());
		verify(persistenceService).save(any(Brokerage.class));
	}

	@Test
	void rejectsCepMismatchBeforeCallingViaCepOrPersisting() {
		when(cnpjLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CnpjRegistrationData("04252011000110", "Razão Social", "Nome", "01001000")));
		when(cvmLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CvmParticipantData("04252011000110", "CORRETORAS", "EM FUNCIONAMENTO NORMAL")));

		BrokerageRuleException exception = assertThrows(BrokerageRuleException.class,
				() -> service.register(OWNER_ID, validRequest()));

		assertEquals("CEP_CNPJ_MISMATCH", exception.getCode());
		verify(addressLookup, never()).findByCep(any());
		verify(persistenceService, never()).save(any());
	}

	@Test
	void translatesExternalFailureAndDoesNotPersistPartialData() {
		when(cnpjLookup.findByCnpj("04252011000110"))
				.thenThrow(new com.bominvestidor.spring.integration.cnpj.CnpjProviderUnavailableException());

		BrokerageProviderUnavailableException exception = assertThrows(BrokerageProviderUnavailableException.class,
				() -> service.register(OWNER_ID, validRequest()));

		assertEquals("CNPJ_PROVIDER_UNAVAILABLE", exception.getCode());
		verify(persistenceService, never()).save(any());
	}

	@Test
	void rejectsUnknownCnpjBeforeCallingOtherSources() {
		when(cnpjLookup.findByCnpj("04252011000110")).thenReturn(Optional.empty());

		BrokerageRuleException exception = assertThrows(BrokerageRuleException.class,
				() -> service.register(OWNER_ID, validRequest()));

		assertEquals("CNPJ_NOT_FOUND", exception.getCode());
		verify(cvmLookup, never()).findByCnpj(any());
		verify(addressLookup, never()).findByCep(any());
		verify(persistenceService, never()).save(any());
	}

	@Test
	void rejectsCnpjThatIsNotAnActiveCvmParticipant() {
		when(cnpjLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CnpjRegistrationData("04252011000110", "Razão Social", "Nome", "04547000")));
		when(cvmLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CvmParticipantData("04252011000110", "CORRETORAS", "CANCELADA")));

		BrokerageRuleException exception = assertThrows(BrokerageRuleException.class,
				() -> service.register(OWNER_ID, validRequest()));

		assertEquals("CNPJ_NOT_ACTIVE_AT_CVM", exception.getCode());
		verify(addressLookup, never()).findByCep(any());
		verify(persistenceService, never()).save(any());
	}

	@Test
	void rejectsUnknownCepAfterCnpjAndCvmValidation() {
		when(cnpjLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CnpjRegistrationData("04252011000110", "Razão Social", "Nome", "04547000")));
		when(cvmLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CvmParticipantData("04252011000110", "CORRETORAS", "EM FUNCIONAMENTO NORMAL")));
		when(addressLookup.findByCep("04547000")).thenReturn(Optional.empty());

		BrokerageRuleException exception = assertThrows(BrokerageRuleException.class,
				() -> service.register(OWNER_ID, validRequest()));

		assertEquals("CEP_NOT_FOUND", exception.getCode());
		verify(persistenceService, never()).save(any());
	}

	@Test
	void rejectsPreliminaryDuplicateWithoutCallingExternalSources() {
		when(brokerageRepository.existsByOwner_IdAndCnpj(OWNER_ID, "04252011000110")).thenReturn(true);

		BrokerageConflictException exception = assertThrows(BrokerageConflictException.class,
				() -> service.register(OWNER_ID, validRequest()));

		assertEquals("BROKERAGE_CNPJ_ALREADY_REGISTERED", exception.getCode());
		verifyNoExternalCalls();
	}

	@Test
	void rejectsDuplicateNicknameIgnoringCase() {
		when(brokerageRepository.existsByOwner_IdAndNicknameKey(OWNER_ID, "minha corretora")).thenReturn(true);

		BrokerageConflictException exception = assertThrows(BrokerageConflictException.class,
				() -> service.register(OWNER_ID, validRequest()));

		assertEquals("BROKERAGE_NICKNAME_ALREADY_REGISTERED", exception.getCode());
		verifyNoExternalCalls();
	}

	@Test
	void listsOnlyTheRequestedOwner() {
		Brokerage brokerage = sampleBrokerage();
		when(persistenceService.findAllByOwner(OWNER_ID)).thenReturn(List.of(brokerage));

		assertEquals(List.of("Minha Corretora"), service.findAll(OWNER_ID).stream().map(response -> response.nickname()).toList());
	}

	@Test
	void looksUpCepWithoutPersistingAndPreservesMissingOptionalFields() {
		when(addressLookup.findByCep("04547000"))
				.thenReturn(Optional.of(new AddressLookupData("04547-000", null, "Bairro", "São Paulo", "SP")));

		var response = service.lookupCep(OWNER_ID, "04547-000");

		assertEquals("04547-000", response.cep());
		assertEquals(null, response.street());
		assertEquals("Bairro", response.neighborhood());
		verify(persistenceService, never()).save(any());
	}

	@Test
	void translatesCepNotFoundAndProviderFailureWithoutPersisting() {
		when(addressLookup.findByCep("04547000")).thenReturn(Optional.empty());
		assertThrows(CepNotFoundException.class, () -> service.lookupCep(OWNER_ID, "04547000"));

		when(addressLookup.findByCep("01001000")).thenThrow(new AddressProviderUnavailableException());
		assertThrows(BrokerageProviderUnavailableException.class, () -> service.lookupCep(OWNER_ID, "01001000"));
		verify(persistenceService, never()).save(any());
	}

	@Test
	void looksUpOfficialCompanyNameBeforeRegistration() {
		when(cnpjLookup.findByCnpj("04252011000110"))
				.thenReturn(Optional.of(new CnpjRegistrationData("04252011000110", "Razão Social", "Nome", "04547000")));

		var response = service.lookupCnpj(OWNER_ID, "04.252.011/0001-10");

		assertEquals("Razão Social", response.legalName());
		assertEquals("Nome", response.tradeName());
	}

	@Test
	void deletesOnlyAnUnlinkedOwnedBrokerage() {
		UUID brokerageId = UUID.randomUUID();
		BrokerageEntity entity = mock(BrokerageEntity.class);
		when(brokerageRepository.findByIdAndOwner_Id(brokerageId, OWNER_ID)).thenReturn(Optional.of(entity));

		service.delete(OWNER_ID, brokerageId);

		verify(brokerageRepository).delete(entity);
		verify(brokerageRepository).flush();
	}

	@Test
	void rejectsDeletionWhenBrokerageHasPortfolio() {
		UUID brokerageId = UUID.randomUUID();
		when(brokerageRepository.findByIdAndOwner_Id(brokerageId, OWNER_ID))
				.thenReturn(Optional.of(mock(BrokerageEntity.class)));
		when(portfolioRepository.existsByBrokerage_Id(brokerageId)).thenReturn(true);

		BrokerageConflictException exception = assertThrows(BrokerageConflictException.class,
				() -> service.delete(OWNER_ID, brokerageId));

		assertEquals("BROKERAGE_HAS_PORTFOLIOS", exception.getCode());
		verify(brokerageRepository, never()).delete(any());
	}

	@Test
	void translatesConcurrentPortfolioLinkDuringDeletion() {
		UUID brokerageId = UUID.randomUUID();
		BrokerageEntity entity = mock(BrokerageEntity.class);
		when(brokerageRepository.findByIdAndOwner_Id(brokerageId, OWNER_ID)).thenReturn(Optional.of(entity));
		doThrow(new DataIntegrityViolationException("foreign key")).when(brokerageRepository).flush();

		BrokerageConflictException exception = assertThrows(BrokerageConflictException.class,
				() -> service.delete(OWNER_ID, brokerageId));

		assertEquals("BROKERAGE_HAS_PORTFOLIOS", exception.getCode());
	}

	private BrokerageCreateRequest validRequest() {
		return new BrokerageCreateRequest("Minha Corretora", "04.252.011/0001-10", "04547-000", "Rua manual",
				"Bairro manual", "42", "Sala 1");
	}

	private UserEntity user() {
		return new UserEntity(OWNER_ID, "Investidor", "investidor@example.com", "hash", UserRole.INVESTOR, NOW, NOW);
	}

	private Brokerage sampleBrokerage() {
		return new Brokerage(UUID.randomUUID(), OWNER_ID, "Minha Corretora", "minha corretora", "04252011000110",
				"Razão Social", "Nome", "EM FUNCIONAMENTO NORMAL", "CORRETORAS",
				new Address("04547000", "Rua manual", "Bairro manual", "42", "Sala 1", "São Paulo", "SP"), NOW, NOW);
	}

	private void verifyNoExternalCalls() {
		verify(cnpjLookup, never()).findByCnpj(any());
		verify(cvmLookup, never()).findByCnpj(any());
		verify(addressLookup, never()).findByCep(any());
		verify(persistenceService, never()).save(any());
	}
}
