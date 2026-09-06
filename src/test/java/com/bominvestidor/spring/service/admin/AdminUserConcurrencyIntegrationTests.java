package com.bominvestidor.spring.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;
import com.bominvestidor.spring.dto.admin.AdminUserUpdateRequest;
import com.bominvestidor.spring.entity.user.UserEntity;
import com.bominvestidor.spring.exception.AdminUserConflictException;
import com.bominvestidor.spring.repository.user.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class AdminUserConcurrencyIntegrationTests {

	@Autowired private AdminUserService service;
	@Autowired private UserRepository users;

	@BeforeEach
	void clearUsers() {
		users.deleteAll();
	}

	@AfterEach
	void clearUsersAfterTest() {
		users.deleteAll();
	}

	@Test
	void concurrentDemotionsKeepOneActiveAdministrator() throws Exception {
		UserEntity first = activeAdministrator("first@example.com");
		UserEntity second = activeAdministrator("second@example.com");

		List<Throwable> outcomes = runConcurrently(
				() -> service.update(first.getId(), second.getId(), investorUpdate("second@example.com")),
				() -> service.update(second.getId(), first.getId(), investorUpdate("first@example.com")));

		assertOneOperationIsRejectedAsLastActiveAdministrator(outcomes);
	}

	@Test
	void concurrentDeactivationsKeepOneActiveAdministrator() throws Exception {
		UserEntity first = activeAdministrator("first@example.com");
		UserEntity second = activeAdministrator("second@example.com");

		List<Throwable> outcomes = runConcurrently(
				() -> service.deactivate(first.getId(), second.getId()),
				() -> service.deactivate(second.getId(), first.getId()));

		assertOneOperationIsRejectedAsLastActiveAdministrator(outcomes);
	}

	private void assertOneOperationIsRejectedAsLastActiveAdministrator(List<Throwable> outcomes) {
		assertEquals(1, outcomes.stream().filter(outcome -> outcome == null).count());
		assertEquals(1, outcomes.stream().filter(this::isLastActiveAdministratorConflict).count());
		assertEquals(1, users.countByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE));
	}

	private boolean isLastActiveAdministratorConflict(Throwable outcome) {
		return outcome instanceof AdminUserConflictException exception
				&& "LAST_ACTIVE_ADMIN".equals(exception.getCode());
	}

	private List<Throwable> runConcurrently(Runnable firstAction, Runnable secondAction) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			List<Future<Throwable>> futures = List.of(
					executor.submit(() -> runWhenReleased(firstAction, ready, start)),
					executor.submit(() -> runWhenReleased(secondAction, ready, start)));
			assertTrue(ready.await(5, TimeUnit.SECONDS));
			start.countDown();
			return Arrays.asList(futures.get(0).get(10, TimeUnit.SECONDS), futures.get(1).get(10, TimeUnit.SECONDS));
		}
		finally {
			executor.shutdownNow();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		}
	}

	private Throwable runWhenReleased(Runnable action, CountDownLatch ready, CountDownLatch start) {
		ready.countDown();
		try {
			start.await();
			action.run();
			return null;
		}
		catch (Throwable exception) {
			return exception;
		}
	}

	private UserEntity activeAdministrator(String email) {
		Instant now = Instant.now();
		return users.saveAndFlush(new UserEntity(UUID.randomUUID(), "Admin", email, "hash", UserRole.ADMIN,
				UserStatus.ACTIVE, now, now));
	}

	private AdminUserUpdateRequest investorUpdate(String email) {
		return new AdminUserUpdateRequest("Admin", email, "password123", UserRole.INVESTOR);
	}
}
