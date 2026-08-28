package com.bominvestidor.spring.domain.portfolio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Portfolio(
		UUID id,
		UUID ownerId,
		UUID brokerageId,
		String name,
		String nameKey,
		Instant createdAt,
		Instant updatedAt) {

	public Portfolio {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(ownerId, "ownerId must not be null");
		Objects.requireNonNull(brokerageId, "brokerageId must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Objects.requireNonNull(nameKey, "nameKey must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}
