package com.bominvestidor.spring.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.dto.auth.UpdateProfileRequest;
import com.bominvestidor.spring.dto.user.PublicUserResponse;
import com.bominvestidor.spring.exception.DuplicateEmailException;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;

@SpringBootTest(properties = {
		"app.security.jwt.secret=test-only-secret-key-with-at-least-32-bytes",
		"app.security.jwt.expiration=PT15M"
})
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "runPostgresTests", matches = "true")
class PostgresAuthenticationIntegrationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void initializesSchemaAndValidatesAuthenticationFlow() {
		String email = "postgres-test-" + UUID.randomUUID() + "@example.com";
		PublicUserResponse registered = null;

		try {
			registered = authService.register(new RegisterRequest("Postgres Test", email, "password123"));
			assertEquals(email, registered.email());
			assertThrows(DuplicateEmailException.class,
					() -> authService.register(new RegisterRequest("Duplicate", email.toUpperCase(), "password456")));

			var authentication = authService.login(new LoginRequest(email, "password123"));
			Jwt jwt = jwtDecoder.decode(authentication.token());
			PublicUserResponse currentUser = authService.currentUser(jwt.getSubject());

			assertEquals(registered.id(), currentUser.id());
			assertEquals(registered.email(), currentUser.email());
		}
		finally {
			if (registered != null) {
				userRepository.deleteById(registered.id());
			}
		}
	}

	@Test
	void updatesPasswordAndTranslatesConcurrentProfileEmailConflict() throws Exception {
		String suffix = UUID.randomUUID().toString();
		PublicUserResponse first = authService.register(
				new RegisterRequest("First", "first-" + suffix + "@example.com", "password123"));
		PublicUserResponse second = authService.register(
				new RegisterRequest("Second", "second-" + suffix + "@example.com", "password456"));
		var executor = Executors.newFixedThreadPool(2);
		try {
			authService.updateCurrentUser(first.id().toString(), new UpdateProfileRequest(
					"First Updated", first.email(), "password123", "new-password"));
			assertTrue(passwordEncoder.matches("new-password",
					userRepository.findById(first.id()).orElseThrow().getPasswordHash()));

			String sharedEmail = "shared-" + suffix + "@example.com";
			CountDownLatch start = new CountDownLatch(1);
			var firstUpdate = executor.submit(() -> updateProfileAfter(start, first.id(), "First", sharedEmail));
			var secondUpdate = executor.submit(() -> updateProfileAfter(start, second.id(), "Second", sharedEmail));
			start.countDown();

			List<String> results = List.of(firstUpdate.get(), secondUpdate.get()).stream().sorted().toList();
			assertEquals(List.of("EMAIL_ALREADY_REGISTERED", "UPDATED"), results);
			assertEquals(1, userRepository.findAll().stream()
					.filter(user -> sharedEmail.equals(user.getEmail())).count());
		}
		finally {
			executor.shutdownNow();
			userRepository.deleteById(first.id());
			userRepository.deleteById(second.id());
		}
	}

	private String updateProfileAfter(CountDownLatch start, UUID userId, String name, String email) throws Exception {
		start.await();
		try {
			authService.updateCurrentUser(userId.toString(), new UpdateProfileRequest(name, email, null, null));
			return "UPDATED";
		}
		catch (DuplicateEmailException exception) {
			return "EMAIL_ALREADY_REGISTERED";
		}
	}
}
