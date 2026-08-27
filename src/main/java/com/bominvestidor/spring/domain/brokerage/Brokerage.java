package com.bominvestidor.spring.domain.brokerage;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Brokerage(
		UUID id,
		UUID ownerId,
		String nickname,
		String nicknameKey,
		String cnpj,
		String legalName,
		String tradeName,
		String registrationStatus,
		String cvmParticipantCategory,
		Address address,
		Instant createdAt,
		Instant updatedAt) {

	public Brokerage {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(ownerId, "ownerId must not be null");
		Objects.requireNonNull(nickname, "nickname must not be null");
		Objects.requireNonNull(nicknameKey, "nicknameKey must not be null");
		Objects.requireNonNull(cnpj, "cnpj must not be null");
		Objects.requireNonNull(legalName, "legalName must not be null");
		Objects.requireNonNull(registrationStatus, "registrationStatus must not be null");
		Objects.requireNonNull(cvmParticipantCategory, "cvmParticipantCategory must not be null");
		Objects.requireNonNull(address, "address must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
