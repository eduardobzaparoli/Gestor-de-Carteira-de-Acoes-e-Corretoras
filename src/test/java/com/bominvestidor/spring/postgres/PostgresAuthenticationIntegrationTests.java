package com.bominvestidor.spring.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import com.bominvestidor.spring.dto.auth.LoginRequest;
import com.bominvestidor.spring.dto.auth.RegisterRequest;
import com.bominvestidor.spring.dto.user.PublicUserResponse;
import com.bominvestidor.spring.exception.DuplicateEmailException;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.auth.AuthService;

@SpringBootTest
@ActiveProfiles("postgres")
@EnabledIfSystemProperty(named = "runPostgresTests", matches = "true")
class PostgresAuthenticationIntegrationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtDecoder jwtDecoder;

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
}
