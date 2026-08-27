package com.bominvestidor.spring.dto.brokerage;

import java.time.Instant;
import java.util.UUID;

public record BrokerageResponse(
		UUID id,
		String nickname,
		String cnpj,
		String legalName,
		String tradeName,
		String registrationStatus,
		String cvmParticipantCategory,
		BrokerageAddressResponse address,
		Instant createdAt,
		Instant updatedAt) {
}
