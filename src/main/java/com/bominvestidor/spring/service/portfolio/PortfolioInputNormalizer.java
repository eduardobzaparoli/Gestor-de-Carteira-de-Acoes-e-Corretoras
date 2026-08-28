package com.bominvestidor.spring.service.portfolio;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.dto.error.FieldErrorResponse;
import com.bominvestidor.spring.dto.portfolio.PortfolioCreateRequest;
import com.bominvestidor.spring.exception.InvalidPortfolioDataException;

@Component
public class PortfolioInputNormalizer {

	public NormalizedPortfolioInput normalize(PortfolioCreateRequest request) {
		String name = request == null || request.name() == null ? null : request.name().trim();
		if (name == null || name.isBlank()) {
			throw new InvalidPortfolioDataException(List.of(new FieldErrorResponse("name", "name is required")));
		}
		if (name.length() > 100) {
			throw new InvalidPortfolioDataException(
					List.of(new FieldErrorResponse("name", "name must have at most 100 characters")));
		}
		if (request.brokerageId() == null) {
			throw new InvalidPortfolioDataException(List.of(new FieldErrorResponse("brokerageId", "brokerageId is required")));
		}
		return new NormalizedPortfolioInput(name, name.toLowerCase(Locale.ROOT), request.brokerageId());
	}
}
