package com.bominvestidor.spring.service.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.user.User;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.auth.AuthenticationResponse;
import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.dto.user.PublicUserResponse;
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

@Service
public class AuthService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_users_email";

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final UserDataNormalizer normalizer;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final Clock clock;

	public AuthService(UserRepository userRepository, UserMapper userMapper, UserDataNormalizer normalizer,
			PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService, Clock clock) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.normalizer = normalizer;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.clock = clock;
	}

	@Transactional
	public PublicUserResponse register(RegisterRequest request) {
		String name = normalizer.normalizeName(request.name());
		String email = normalizer.normalizeEmail(request.email());
		validateRegistration(name, email, request.password());

		if (userRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}

		Instant now = clock.instant();
		User user = new User(
				UUID.randomUUID(),
				name,
				email,
				passwordEncoder.encode(request.password()),
				UserRole.INVESTOR,
				now,
				now);

		try {
			UserEntity saved = userRepository.saveAndFlush(userMapper.toEntity(user));
			return userMapper.toPublicResponse(userMapper.toDomain(saved));
		}
		catch (DataIntegrityViolationException exception) {
			if (isEmailUniqueConstraint(exception)) {
				throw new DuplicateEmailException();
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public AuthenticationResponse login(LoginRequest request) {
		String email = normalizer.normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
				.map(userMapper::toDomain)
				.filter(found -> passwordEncoder.matches(request.password(), found.passwordHash()))
				.orElseThrow(InvalidCredentialsException::new);
		IssuedToken issuedToken = jwtTokenService.issue(user);

		return new AuthenticationResponse(
				issuedToken.value(),
				"Bearer",
				issuedToken.expiresAt(),
				userMapper.toPublicResponse(user));
	}

	@Transactional(readOnly = true)
	public PublicUserResponse currentUser(String subject) {
		UUID userId;
		try {
			userId = UUID.fromString(subject);
		}
		catch (IllegalArgumentException exception) {
			throw new AuthenticatedUserNotFoundException();
		}

		return userRepository.findById(userId)
				.map(userMapper::toDomain)
				.map(userMapper::toPublicResponse)
				.orElseThrow(AuthenticatedUserNotFoundException::new);
	}

	private void validateRegistration(String name, String email, String password) {
		if (name == null || name.isBlank()) {
			throw new InvalidUserDataException("name", "Name is required");
		}
		if (name.length() > 100) {
			throw new InvalidUserDataException("name", "Name must have at most 100 characters");
		}
		if (email == null || email.isBlank()) {
			throw new InvalidUserDataException("email", "Email is required");
		}
		if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
			throw new InvalidUserDataException("email", "Email must be valid and have at most 254 characters");
		}
		if (password == null || password.length() < 8 || password.length() > 72) {
			throw new InvalidUserDataException("password", "Password must have between 8 and 72 characters");
		}
	}

	private boolean isEmailUniqueConstraint(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof ConstraintViolationException constraintViolation) {
				String constraintName = constraintViolation.getConstraintName();
				return constraintName != null && EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName);
			}
			current = current.getCause();
		}
		return false;
	}
}
