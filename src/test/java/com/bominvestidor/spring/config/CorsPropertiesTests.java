package com.bominvestidor.spring.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class CorsPropertiesTests {
	@Test void acceptsOnlyExactHttpOrigins() {
		assertDoesNotThrow(() -> new CorsProperties(List.of("https://frontend.example", "http://localhost:3000")));
		assertThrows(IllegalArgumentException.class, () -> new CorsProperties(List.of("*")));
		assertThrows(IllegalArgumentException.class, () -> new CorsProperties(List.of("https://frontend.example/path")));
	}
}
