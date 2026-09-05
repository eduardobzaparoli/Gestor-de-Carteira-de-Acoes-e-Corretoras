package com.bominvestidor.spring.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;
import com.bominvestidor.spring.dto.admin.AdminUserCreateRequest;
import com.bominvestidor.spring.dto.admin.AdminUserUpdateRequest;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AdminUserConflictException;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.user.UserDataNormalizer;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-05T12:00:00Z");

	@Mock private UserRepository users;
	@Mock private PasswordEncoder passwordEncoder;
	private AdminUserService service;

	@BeforeEach
	void setUp() {
		service = new AdminUserService(users, new UserDataNormalizer(), passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void createsNormalizedActiveAccountWithHashedPassword() {
		when(users.existsByEmail("investor@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		when(users.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.create(new AdminUserCreateRequest(" Investor ", " INVESTOR@EXAMPLE.COM ",
				"password123", UserRole.INVESTOR));

		ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
		org.mockito.Mockito.verify(users).saveAndFlush(captor.capture());
		assertEquals("Investor", captor.getValue().getName());
		assertEquals("investor@example.com", captor.getValue().getEmail());
		assertEquals("encoded-password", captor.getValue().getPasswordHash());
		assertEquals(UserStatus.ACTIVE, response.status());
	}

	@Test
	void preventsDemotionOfTheLastActiveAdministrator() {
		UUID adminId = UUID.randomUUID();
		UserEntity admin = user(adminId, UserRole.ADMIN, UserStatus.ACTIVE);
		when(users.findById(adminId)).thenReturn(Optional.of(admin));
		when(users.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE)).thenReturn(1L);

		AdminUserConflictException exception = assertThrows(AdminUserConflictException.class,
				() -> service.update(UUID.randomUUID(), adminId,
						new AdminUserUpdateRequest("Admin", "admin@example.com", "password123", UserRole.INVESTOR)));

		assertEquals("LAST_ACTIVE_ADMIN", exception.getCode());
	}

	@Test
	void preventsAnAdministratorFromDeactivatingThemselves() {
		UUID adminId = UUID.randomUUID();
		when(users.findById(adminId)).thenReturn(Optional.of(user(adminId, UserRole.ADMIN, UserStatus.ACTIVE)));

		AdminUserConflictException exception = assertThrows(AdminUserConflictException.class,
				() -> service.deactivate(adminId, adminId));

		assertEquals("CANNOT_DEACTIVATE_SELF", exception.getCode());
	}

	private UserEntity user(UUID id, UserRole role, UserStatus status) {
		return new UserEntity(id, "Admin", "admin@example.com", "hash", role, status, NOW, NOW);
	}
}
