package com.bominvestidor.spring.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.repository.user.UserRepository;
import com.bominvestidor.spring.service.user.UserDataNormalizer;

@Component
public class AdminBootstrapInitializer implements ApplicationRunner {

	private final AdminBootstrapProperties properties;
	private final UserRepository userRepository;
	private final UserDataNormalizer normalizer;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public AdminBootstrapInitializer(AdminBootstrapProperties properties, UserRepository userRepository,
			UserDataNormalizer normalizer, PasswordEncoder passwordEncoder, Clock clock) {
		this.properties = properties;
		this.userRepository = userRepository;
		this.normalizer = normalizer;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (properties.isAbsent()) {
			return;
		}
		if (!properties.isComplete()) {
			throw new IllegalStateException("Admin bootstrap requires name, email and password");
		}

		String email = normalizer.normalizeEmail(properties.email());
		if (userRepository.existsByEmail(email)) {
			return;
		}

		Instant now = clock.instant();
		UserEntity admin = new UserEntity(UUID.randomUUID(), normalizer.normalizeName(properties.name()), email,
				passwordEncoder.encode(properties.password()), UserRole.ADMIN, UserStatus.ACTIVE, now, now);
		userRepository.save(admin);
	}
}
