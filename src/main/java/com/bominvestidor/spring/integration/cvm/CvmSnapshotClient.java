package com.bominvestidor.spring.integration.cvm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;

@Component
public class CvmSnapshotClient implements CvmParticipantStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(CvmSnapshotClient.class);

	private final RestClient restClient;
	private final BrokerageIntegrationProperties properties;
	private final Clock clock;
	private volatile Cache cache;

	public CvmSnapshotClient(@Qualifier("cvmRestClient") RestClient restClient,
			BrokerageIntegrationProperties properties, Clock clock) {
		this.restClient = restClient;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public Optional<CvmParticipantData> findByCnpj(String cnpj) {
		Cache current = cache;
		if (isValid(current)) {
			return Optional.ofNullable(current.participants().get(cnpj));
		}
		synchronized (this) {
			current = cache;
			if (!isValid(current)) {
				cache = current = refresh();
			}
		}
		return Optional.ofNullable(current.participants().get(cnpj));
	}

	private boolean isValid(Cache value) {
		return value != null && value.loadedAt().plus(properties.getCvmCacheTtl()).isAfter(clock.instant());
	}

	private Cache refresh() {
		try {
			byte[] archive = restClient.get().uri(properties.getCvmSnapshotUrl()).retrieve().body(byte[].class);
			if (archive == null || archive.length == 0 || archive.length > properties.getCvmMaxSnapshotBytes()) {
				throw new IOException("Invalid CVM snapshot size");
			}
			Map<String, CvmParticipantData> participants = CvmSnapshotParser
					.parseZip(new ByteArrayInputStream(archive));
			if (participants.isEmpty()) {
				throw new IOException("CVM snapshot has no valid participants");
			}
			return new Cache(participants, clock.instant());
		}
		catch (IOException | RuntimeException exception) {
			LOGGER.warn("CVM snapshot unavailable");
			throw new CvmProviderUnavailableException();
		}
	}

	private record Cache(Map<String, CvmParticipantData> participants, Instant loadedAt) {
	}
}
