package com.bominvestidor.spring.dto.auth;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 100, message = "Name must have at most 100 characters")
		String name,
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 254, message = "Email must have at most 254 characters")
		String email,
		String currentPassword,
		@Size(min = 8, max = 72, message = "New password must have between 8 and 72 characters")
		String newPassword) {

	public UpdateProfileRequest {
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
