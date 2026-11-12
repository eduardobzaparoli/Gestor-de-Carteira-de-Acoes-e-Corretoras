package com.bominvestidor.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtPropertiesTests {

	private static final String SECRET = "test-only-secret-key-with-at-least-32-bytes";

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "short" })
	void requiresASecretWithAtLeast32Bytes(String secret) {
		assertThrows(IllegalStateException.class,
				() -> new JwtProperties(secret, Duration.ofHours(1), "bom-investidor-test"));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(longs = { 0, -1 })
	void requiresAPositiveExpiration(Long seconds) {
		Duration expiration = seconds == null ? null : Duration.ofSeconds(seconds);

		assertThrows(IllegalStateException.class,
				() -> new JwtProperties(SECRET, expiration, "bom-investidor-test"));
	}

	@Test
	void requiresANonBlankIssuer() {
		assertThrows(IllegalStateException.class, () -> new JwtProperties(SECRET, Duration.ofHours(1), null));
		assertThrows(IllegalStateException.class, () -> new JwtProperties(SECRET, Duration.ofHours(1), "  "));
	}

	@Test
	void acceptsAConfiguredIssuer() {
		assertEquals("bom-investidor-test",
				new JwtProperties(SECRET, Duration.ofHours(1), "bom-investidor-test").issuer());
	}
}
