package com.bominvestidor.spring.security;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.user.User;

@Service
public class JwtTokenService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public IssuedToken issue(User user) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(properties.expiration());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.id().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("role", user.role().name())
				.build();
		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();

		return new IssuedToken(value, expiresAt);
	}
}
