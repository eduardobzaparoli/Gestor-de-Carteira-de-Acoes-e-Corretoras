package com.bominvestidor.spring.dto.admin;

import java.time.Instant;
import java.util.UUID;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;

public record AdminUserResponse(UUID id, String name, String email, UserRole role, UserStatus status,
		Instant createdAt, Instant updatedAt) {
}
