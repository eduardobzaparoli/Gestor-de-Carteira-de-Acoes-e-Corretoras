package com.bominvestidor.spring.service.user;

import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class UserDataNormalizer {

	public String normalizeName(String name) {
		return name == null ? null : name.trim();
	}

	public String normalizeEmail(String email) {
		String normalized = email == null ? null : email.trim();
		return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
	}
}
