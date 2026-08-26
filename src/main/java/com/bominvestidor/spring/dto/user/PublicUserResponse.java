package com.bominvestidor.spring.dto.user;

import java.util.UUID;

import com.bominvestidor.spring.domain.user.UserRole;

public record PublicUserResponse(UUID id, String name, String email, UserRole role) {
}
