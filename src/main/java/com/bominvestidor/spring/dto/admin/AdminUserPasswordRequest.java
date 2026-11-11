package com.bominvestidor.spring.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUserPasswordRequest(
		@NotBlank(message = "Password is required") @Size(min = 8, max = 72, message = "Password must have between 8 and 72 characters") String password) {
}
