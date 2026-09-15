package com.bominvestidor.spring.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, Duration expiration) {

	private static final int MINIMUM_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET must be configured");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
		}
		if (expiration == null || expiration.isZero() || expiration.isNegative()) {
			throw new IllegalStateException("JWT expiration must be positive");
		}
	}
}
