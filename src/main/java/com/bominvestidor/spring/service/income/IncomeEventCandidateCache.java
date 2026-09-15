package com.bominvestidor.spring.service.income;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.income.IncomeEventCandidate;

@Component
public class IncomeEventCandidateCache {
	private final ConcurrentHashMap<UUID, Entry> entries = new ConcurrentHashMap<>();
	private final Clock clock;
	private final Duration ttl;

	public IncomeEventCandidateCache(Clock clock, BrokerageIntegrationProperties properties) {
		this.clock = clock;
		this.ttl = properties.getIncomeCandidateCacheTtl();
	}

	public UUID store(UUID ownerId, UUID portfolioId, IncomeEventCandidate candidate) {
		UUID id = UUID.randomUUID();
		entries.put(id, new Entry(ownerId, portfolioId, candidate, clock.instant().plus(ttl)));
		return id;
	}

	public Optional<IncomeEventCandidate> find(UUID id, UUID ownerId, UUID portfolioId) {
		Entry entry = entries.get(id);
		if (entry == null || !entry.ownerId().equals(ownerId) || !entry.portfolioId().equals(portfolioId)
				|| !entry.expiresAt().isAfter(clock.instant())) {
			entries.remove(id);
			return Optional.empty();
		}
		return Optional.of(entry.candidate());
	}

	public void remove(UUID id) { entries.remove(id); }
	private record Entry(UUID ownerId, UUID portfolioId, IncomeEventCandidate candidate, Instant expiresAt) { }
}
