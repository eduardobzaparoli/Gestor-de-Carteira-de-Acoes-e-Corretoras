package com.bominvestidor.spring.dto.portfolio;

import java.util.UUID;

public record BrokerageSummaryResponse(UUID id, String nickname, String cnpj, String legalName) {
}
