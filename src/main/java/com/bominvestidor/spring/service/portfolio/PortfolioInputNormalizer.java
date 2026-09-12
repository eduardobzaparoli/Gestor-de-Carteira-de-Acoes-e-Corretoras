package com.bominvestidor.spring.service.portfolio;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.dto.error.FieldErrorResponse;
import com.bominvestidor.spring.dto.portfolio.PortfolioCreateRequest;
import com.bominvestidor.spring.dto.portfolio.PortfolioUpdateRequest;
import com.bominvestidor.spring.exception.InvalidPortfolioDataException;

@Component
public class PortfolioInputNormalizer {

	public NormalizedPortfolioInput normalize(PortfolioCreateRequest request) {
		return normalize(request == null ? null : request.name(), request == null ? null : request.brokerageId());
	}

	public NormalizedPortfolioInput normalize(PortfolioUpdateRequest request) {
		return normalize(request == null ? null : request.name(), request == null ? null : request.brokerageId());
	}

	private NormalizedPortfolioInput normalize(String rawName, java.util.UUID brokerageId) {
		String name = rawName == null ? null : rawName.trim();
		if (name == null || name.isBlank()) {
			throw new InvalidPortfolioDataException(List.of(new FieldErrorResponse("name", "name is required")));
		}
		if (name.length() > 100) {
			throw new InvalidPortfolioDataException(
					List.of(new FieldErrorResponse("name", "name must have at most 100 characters")));
		}
		if (brokerageId == null) {
			throw new InvalidPortfolioDataException(List.of(new FieldErrorResponse("brokerageId", "brokerageId is required")));
		}
		return new NormalizedPortfolioInput(name, name.toLowerCase(Locale.ROOT), brokerageId);
	}
}
