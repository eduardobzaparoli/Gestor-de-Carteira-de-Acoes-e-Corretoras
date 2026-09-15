package com.bominvestidor.spring.dto.auth;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		@Size(max = 254, message = "Email must have at most 254 characters")
		String email,
		@NotBlank(message = "Password is required")
		@Size(max = 72, message = "Password must have at most 72 characters")
		String password) {

	public LoginRequest {
		email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
