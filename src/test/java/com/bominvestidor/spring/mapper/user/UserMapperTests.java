package com.bominvestidor.spring.mapper.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.user.User;
import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.dto.user.PublicUserResponse;
import com.bominvestidor.spring.entity.user.UserEntity;

class UserMapperTests {

	private final UserMapper mapper = new UserMapper();

	@Test
	void mapsBetweenDomainAndEntity() {
		Instant createdAt = Instant.parse("2026-08-25T12:00:00Z");
		Instant updatedAt = Instant.parse("2026-08-25T13:00:00Z");
		User domain = new User(UUID.randomUUID(), "Eduardo", "eduardo@example.com", "hash",
				UserRole.INVESTOR, createdAt, updatedAt);

		UserEntity entity = mapper.toEntity(domain);
		User remapped = mapper.toDomain(entity);

		assertEquals(domain, remapped);
	}

	@Test
	void publicResponseContainsOnlyPublicFields() {
		Instant now = Instant.now();
		User domain = new User(UUID.randomUUID(), "Eduardo", "eduardo@example.com", "sensitive-hash",
				UserRole.INVESTOR, now, now);

		PublicUserResponse response = mapper.toPublicResponse(domain);
		Set<String> componentNames = Arrays.stream(PublicUserResponse.class.getRecordComponents())
				.map(component -> component.getName())
				.collect(Collectors.toSet());

		assertEquals(domain.id(), response.id());
		assertEquals(domain.name(), response.name());
		assertEquals(domain.email(), response.email());
		assertEquals(domain.role(), response.role());
		assertFalse(componentNames.contains("password"));
		assertFalse(componentNames.contains("passwordHash"));
	}
}
