package com.bominvestidor.spring.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.AuthenticationResponse;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AuthenticatedUserNotFoundException;
import com.bominvestidor.spring.exception.DuplicateEmailException;
import com.bominvestidor.spring.exception.InvalidCredentialsException;
import com.bominvestidor.spring.exception.InvalidUserDataException;
import com.bominvestidor.spring.mapper.user.UserMapper;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.security.IssuedToken;
import com.bominvestidor.spring.security.JwtTokenService;
import com.bominvestidor.spring.service.user.UserDataNormalizer;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenService jwtTokenService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				userRepository,
				new UserMapper(),
				new UserDataNormalizer(),
				passwordEncoder,
				jwtTokenService,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void registersNormalizedInvestorWithHashedPassword() {
		when(userRepository.existsByEmail("investidor@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = authService.register(
				new RegisterRequest("  Investidor  ", " INVESTIDOR@EXAMPLE.COM ", "password123"));

		ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
		verify(userRepository).saveAndFlush(entityCaptor.capture());
		UserEntity saved = entityCaptor.getValue();
		assertEquals("Investidor", saved.getName());
		assertEquals("investidor@example.com", saved.getEmail());
		assertEquals("encoded-password", saved.getPasswordHash());
		assertEquals(UserRole.INVESTOR, saved.getRole());
		assertEquals(UserRole.INVESTOR, response.role());
	}

	@Test
	void rejectsInvalidFieldsAfterNormalization() {
		assertThrows(InvalidUserDataException.class,
				() -> authService.register(new RegisterRequest("   ", "valid@example.com", "password123")));
		assertThrows(InvalidUserDataException.class,
				() -> authService.register(new RegisterRequest("a".repeat(101), "valid@example.com", "password123")));
		assertThrows(InvalidUserDataException.class,
				() -> authService.register(new RegisterRequest("Valid", "invalid-email", "password123")));
		assertThrows(InvalidUserDataException.class,
				() -> authService.register(new RegisterRequest("Valid", "valid@example.com", "short")));
		verify(userRepository, never()).saveAndFlush(any());
	}

	@Test
	void rejectsPreviouslyRegisteredEmail() {
		when(userRepository.existsByEmail("investidor@example.com")).thenReturn(true);

		assertThrows(DuplicateEmailException.class,
				() -> authService.register(new RegisterRequest("Investidor", "INVESTIDOR@example.com", "password123")));
	}

	@Test
	void translatesNamedUniqueConstraintViolation() {
		when(userRepository.existsByEmail("investidor@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		ConstraintViolationException cause = new ConstraintViolationException(
				"duplicate", new SQLException("duplicate"), "uk_users_email");
		when(userRepository.saveAndFlush(any(UserEntity.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate", cause));

		assertThrows(DuplicateEmailException.class,
				() -> authService.register(new RegisterRequest("Investidor", "investidor@example.com", "password123")));
	}

	@Test
	void authenticatesValidCredentials() {
		UserEntity entity = userEntity("investidor@example.com", "encoded-password");
		when(userRepository.findByEmail("investidor@example.com")).thenReturn(Optional.of(entity));
		when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
		when(jwtTokenService.issue(any())).thenReturn(new IssuedToken("signed-token", NOW.plusSeconds(900)));

		AuthenticationResponse response = authService.login(
				new LoginRequest(" INVESTIDOR@EXAMPLE.COM ", "password123"));

		assertEquals("signed-token", response.token());
		assertEquals("Bearer", response.tokenType());
		assertEquals(NOW.plusSeconds(900), response.expiresAt());
		assertEquals("investidor@example.com", response.user().email());
	}

	@Test
	void usesSameExceptionForUnknownEmailAndWrongPassword() {
		when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
		assertThrows(InvalidCredentialsException.class,
				() -> authService.login(new LoginRequest("unknown@example.com", "password123")));

		UserEntity entity = userEntity("investidor@example.com", "encoded-password");
		when(userRepository.findByEmail("investidor@example.com")).thenReturn(Optional.of(entity));
		when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);
		assertThrows(InvalidCredentialsException.class,
				() -> authService.login(new LoginRequest("investidor@example.com", "wrong-password")));
	}

	@Test
	void rejectsMissingAuthenticatedUser() {
		UUID id = UUID.randomUUID();
		when(userRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(AuthenticatedUserNotFoundException.class, () -> authService.currentUser(id.toString()));
		assertThrows(AuthenticatedUserNotFoundException.class, () -> authService.currentUser("invalid-subject"));
	}

	private UserEntity userEntity(String email, String passwordHash) {
		return new UserEntity(UUID.randomUUID(), "Investidor", email, passwordHash, UserRole.INVESTOR, NOW, NOW);
	}
}
