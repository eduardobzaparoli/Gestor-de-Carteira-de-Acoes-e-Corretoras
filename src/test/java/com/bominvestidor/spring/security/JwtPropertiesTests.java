package com.bominvestidor.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class JwtPropertiesTests {

	private static final String SECRET = "test-only-secret-key-with-at-least-32-bytes";

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
