package com.bominvestidor.spring.dto.admin;

import java.util.Locale;

import com.bominvestidor.spring.domain.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
		@NotBlank(message = "Name is required") @Size(max = 100, message = "Name must have at most 100 characters") String name,
		@NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 254, message = "Email must have at most 254 characters") String email,
		@NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must have between 8 and 72 characters") String password,
		@NotNull(message = "Role is required") UserRole role) {

	public AdminUserUpdateRequest {
		name = trim(name);
		email = normalizeEmail(email);
	}

	private static String trim(String value) {
		return value == null ? null : value.trim();
	}

	private static String normalizeEmail(String value) {
		String trimmed = trim(value);
		return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
	}
}
