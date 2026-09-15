package com.bominvestidor.spring.domain.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record User(
		UUID id,
		String name,
		String email,
		String passwordHash,
		UserRole role,
		UserStatus status,
		Instant createdAt,
		Instant updatedAt) {

	public User {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(email, "email must not be null");
		Objects.requireNonNull(passwordHash, "passwordHash must not be null");
		Objects.requireNonNull(role, "role must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public User(UUID id, String name, String email, String passwordHash, UserRole role,
			Instant createdAt, Instant updatedAt) {
		this(id, name, email, passwordHash, role, UserStatus.ACTIVE, createdAt, updatedAt);
	}
}
