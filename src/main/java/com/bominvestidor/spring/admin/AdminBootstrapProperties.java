package com.bominvestidor.spring.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.bootstrap")
public record AdminBootstrapProperties(String name, String email, String password) {

	public boolean isAbsent() {
		return isBlank(name) && isBlank(email) && isBlank(password);
	}

	public boolean isComplete() {
		return !isBlank(name) && !isBlank(email) && !isBlank(password);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
