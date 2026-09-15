package com.bominvestidor.spring.dto.auth;

import java.time.Instant;

import com.bominvestidor.spring.dto.user.PublicUserResponse;

public record AuthenticationResponse(
		String token,
		String tokenType,
		Instant expiresAt,
		PublicUserResponse user) {
}
