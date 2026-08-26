package com.bominvestidor.spring.repository.user;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.entity.user.UserEntity;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void databaseRejectsDuplicateNormalizedEmail() {
		Instant now = Instant.now();
		userRepository.saveAndFlush(user("investidor@example.com", now));

		assertThrows(DataIntegrityViolationException.class,
				() -> userRepository.saveAndFlush(user("investidor@example.com", now)));
	}

	private UserEntity user(String email, Instant now) {
		return new UserEntity(UUID.randomUUID(), "Investidor", email, "encoded-password",
				UserRole.INVESTOR, now, now);
	}
}
