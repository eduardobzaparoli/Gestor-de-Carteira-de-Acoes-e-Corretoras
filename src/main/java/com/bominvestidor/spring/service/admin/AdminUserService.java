package com.bominvestidor.spring.service.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;
import com.bominvestidor.spring.dto.admin.AdminUserCreateRequest;
import com.bominvestidor.spring.dto.admin.AdminUserResponse;
import com.bominvestidor.spring.dto.admin.AdminUserUpdateRequest;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AdminUserConflictException;
import com.bominvestidor.spring.exception.DuplicateEmailException;
import com.bominvestidor.spring.exception.InvalidUserDataException;
import com.bominvestidor.spring.exception.UserNotFoundException;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.user.UserDataNormalizer;

@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final UserDataNormalizer normalizer;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public AdminUserService(UserRepository userRepository, UserDataNormalizer normalizer,
			PasswordEncoder passwordEncoder, Clock clock) {
		this.userRepository = userRepository;
		this.normalizer = normalizer;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<AdminUserResponse> findAll() {
		return userRepository.findAllByOrderByCreatedAtAscIdAsc().stream().map(this::response).toList();
	}

	@Transactional(readOnly = true)
	public AdminUserResponse findById(UUID userId) {
		return response(findEntity(userId));
	}

	@Transactional
	public AdminUserResponse create(AdminUserCreateRequest request) {
		String name = normalizer.normalizeName(request.name());
		String email = normalizer.normalizeEmail(request.email());
		validate(name, email, request.password(), request.role());
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}

		Instant now = clock.instant();
		UserEntity user = new UserEntity(UUID.randomUUID(), name, email, passwordEncoder.encode(request.password()),
				request.role(), UserStatus.ACTIVE, now, now);
		return response(userRepository.saveAndFlush(user));
	}

	@Transactional
	public AdminUserResponse update(UUID actorId, UUID userId, AdminUserUpdateRequest request) {
		lockActiveAdministrators();
		UserEntity user = findEntity(userId);
		String name = normalizer.normalizeName(request.name());
		String email = normalizer.normalizeEmail(request.email());
		validate(name, email, request.password(), request.role());
		if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
			throw new DuplicateEmailException();
		}
		assertCanKeepAdministrativeContinuity(actorId, user, request.role(), user.getStatus());
		user.update(name, email, passwordEncoder.encode(request.password()), request.role());
		return response(userRepository.saveAndFlush(user));
	}

	@Transactional
	public void deactivate(UUID actorId, UUID userId) {
		lockActiveAdministrators();
		UserEntity user = findEntity(userId);
		if (actorId.equals(userId)) {
			throw new AdminUserConflictException("CANNOT_DEACTIVATE_SELF", "Administrators cannot deactivate themselves");
		}
		if (user.getStatus() == UserStatus.INACTIVE) {
			return;
		}
		assertCanKeepAdministrativeContinuity(actorId, user, user.getRole(), UserStatus.INACTIVE);
		user.changeStatus(UserStatus.INACTIVE);
		userRepository.saveAndFlush(user);
	}

	@Transactional
	public AdminUserResponse reactivate(UUID userId) {
		UserEntity user = findEntity(userId);
		user.changeStatus(UserStatus.ACTIVE);
		return response(userRepository.saveAndFlush(user));
	}

	private UserEntity findEntity(UUID userId) {
		return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
	}

	private void lockActiveAdministrators() {
		userRepository.findAllByRoleAndStatusForUpdate(UserRole.ADMIN, UserStatus.ACTIVE);
	}

	private void assertCanKeepAdministrativeContinuity(UUID actorId, UserEntity user, UserRole requestedRole,
			UserStatus requestedStatus) {
		boolean removesActiveAdmin = user.getRole() == UserRole.ADMIN && user.getStatus() == UserStatus.ACTIVE
				&& (requestedRole != UserRole.ADMIN || requestedStatus != UserStatus.ACTIVE);
		if (removesActiveAdmin && userRepository.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE) <= 1) {
			throw new AdminUserConflictException("LAST_ACTIVE_ADMIN", "At least one active administrator is required");
		}
	}

	private void validate(String name, String email, String password, UserRole role) {
		if (name == null || name.isBlank()) {
			throw new InvalidUserDataException("name", "Name is required");
		}
		if (name.length() > 100) {
			throw new InvalidUserDataException("name", "Name must have at most 100 characters");
		}
		if (email == null || email.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || email.length() > 254) {
			throw new InvalidUserDataException("email", "Email must be valid and have at most 254 characters");
		}
		if (password == null || password.length() < 8 || password.length() > 72) {
			throw new InvalidUserDataException("password", "Password must have between 8 and 72 characters");
		}
		if (role == null) {
			throw new InvalidUserDataException("role", "Role is required");
		}
	}

	private AdminUserResponse response(UserEntity user) {
		return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(),
				user.getCreatedAt(), user.getUpdatedAt());
	}
}
