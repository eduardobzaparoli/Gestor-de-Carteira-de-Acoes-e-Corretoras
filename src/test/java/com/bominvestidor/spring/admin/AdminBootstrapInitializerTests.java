package com.bominvestidor.spring.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.user.UserDataNormalizer;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapInitializerTests {

	@Mock private UserRepository users;
	@Mock private PasswordEncoder passwordEncoder;

	@Test
	void provisionsAnActiveAdministratorOnlyWhenTheConfiguredEmailIsAbsent() {
		when(users.existsByEmail("admin@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
		AdminBootstrapInitializer initializer = initializer(" Admin ", " ADMIN@EXAMPLE.COM ", "password123");

		initializer.run(null);

		ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
		verify(users).save(captor.capture());
		assertEquals("Admin", captor.getValue().getName());
		assertEquals("admin@example.com", captor.getValue().getEmail());
		assertEquals(UserRole.ADMIN, captor.getValue().getRole());
		assertEquals(UserStatus.ACTIVE, captor.getValue().getStatus());
	}

	@Test
	void doesNothingWhenBootstrapIsNotConfiguredOrTheAccountAlreadyExists() throws Exception {
		initializer(null, null, null).run(null);
		verify(users, never()).save(any());

		when(users.existsByEmail("admin@example.com")).thenReturn(true);
		initializer("Admin", "admin@example.com", "password123").run(null);
		verify(users, never()).save(any());
	}

	private AdminBootstrapInitializer initializer(String name, String email, String password) {
		return new AdminBootstrapInitializer(new AdminBootstrapProperties(name, email, password), users,
				new UserDataNormalizer(), passwordEncoder,
				Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC));
	}
}
