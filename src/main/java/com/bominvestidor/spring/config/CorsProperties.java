package com.bominvestidor.spring.config;

import java.util.List;
import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.web.cors")
public record CorsProperties(List<String> allowedOrigins) {
	public CorsProperties {
		allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		allowedOrigins.forEach(CorsProperties::validateOrigin);
	}

	private static void validateOrigin(String origin) {
		URI value;
		try { value = URI.create(origin); }
		catch (IllegalArgumentException exception) { throw new IllegalArgumentException("CORS origin must be an absolute HTTP(S) origin", exception); }
		if (!("http".equalsIgnoreCase(value.getScheme()) || "https".equalsIgnoreCase(value.getScheme()))
				|| value.getHost() == null || value.getUserInfo() != null || value.getQuery() != null
				|| value.getFragment() != null || (value.getPath() != null && !value.getPath().isEmpty()))
			throw new IllegalArgumentException("CORS origin must be an absolute HTTP(S) origin without path, query or wildcard");
	}
}
