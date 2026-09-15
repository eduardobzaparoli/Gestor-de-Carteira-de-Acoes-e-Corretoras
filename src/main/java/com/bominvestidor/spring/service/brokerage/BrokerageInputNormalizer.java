package com.bominvestidor.spring.service.brokerage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bominvestidor.spring.dto.brokerage.BrokerageCreateRequest;
import com.bominvestidor.spring.dto.error.FieldErrorResponse;
import com.bominvestidor.spring.exception.InvalidBrokerageDataException;

import org.springframework.stereotype.Component;

@Component
public class BrokerageInputNormalizer {

	public NormalizedBrokerageInput normalize(BrokerageCreateRequest request) {
		List<FieldErrorResponse> errors = new ArrayList<>();
		String nickname = trim(request == null ? null : request.nickname());
		String cnpj = normalizeDocument(request == null ? null : request.cnpj(), "cnpj", 14, errors);
		String cep = normalizeDocument(request == null ? null : request.cep(), "cep", 8, errors);
		String street = trim(request == null ? null : request.street());
		String neighborhood = trim(request == null ? null : request.neighborhood());
		String number = trim(request == null ? null : request.number());
		String complement = trim(request == null ? null : request.complement());

		validateRequiredAndSize(nickname, "nickname", 100, errors);
		validateRequiredAndSize(street, "street", 150, errors);
		validateRequiredAndSize(neighborhood, "neighborhood", 100, errors);
		validateRequiredAndSize(number, "number", 20, errors);
		validateSize(complement, "complement", 100, errors);
		if (!errors.isEmpty()) {
			throw new InvalidBrokerageDataException(errors);
		}
		return new NormalizedBrokerageInput(nickname, nickname.toLowerCase(Locale.ROOT), cnpj, cep, street, neighborhood,
				number, complement);
	}

	public String normalizeCep(String value) {
		List<FieldErrorResponse> errors = new ArrayList<>();
		String cep = normalizeDocument(value, "cep", 8, errors);
		if (!errors.isEmpty()) {
			throw new InvalidBrokerageDataException(errors);
		}
		return cep;
	}

	public String normalizeCnpj(String value) {
		List<FieldErrorResponse> errors = new ArrayList<>();
		String cnpj = normalizeDocument(value, "cnpj", 14, errors);
		if (!errors.isEmpty()) {
			throw new InvalidBrokerageDataException(errors);
		}
		return cnpj;
	}

	private String normalizeDocument(String value, String field, int expectedLength, List<FieldErrorResponse> errors) {
		String trimmed = trim(value);
		if (trimmed == null || trimmed.isBlank()) {
			errors.add(new FieldErrorResponse(field, field.toUpperCase(Locale.ROOT) + " is required"));
			return "";
		}
		if (!trimmed.matches("[0-9.\\-\\/\\s]+")) {
			errors.add(new FieldErrorResponse(field, field.toUpperCase(Locale.ROOT) + " must contain only digits and formatting"));
			return "";
		}
		String digits = trimmed.replaceAll("\\D", "");
		if (digits.length() != expectedLength || ("cnpj".equals(field) && !isValidCnpj(digits))) {
			errors.add(new FieldErrorResponse(field, "Invalid " + field.toUpperCase(Locale.ROOT)));
		}
		return digits;
	}

	private void validateRequiredAndSize(String value, String field, int max, List<FieldErrorResponse> errors) {
		if (value == null || value.isBlank()) {
			errors.add(new FieldErrorResponse(field, field + " is required"));
		} else {
			validateSize(value, field, max, errors);
		}
	}

	private void validateSize(String value, String field, int max, List<FieldErrorResponse> errors) {
		if (value != null && value.length() > max) {
			errors.add(new FieldErrorResponse(field, field + " must have at most " + max + " characters"));
		}
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}

	private boolean isValidCnpj(String cnpj) {
		if (cnpj.length() != 14 || cnpj.chars().distinct().count() == 1) {
			return false;
		}
		int[] firstWeights = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
		int[] secondWeights = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
		return checkDigit(cnpj, firstWeights, 12) == Character.digit(cnpj.charAt(12), 10)
				&& checkDigit(cnpj, secondWeights, 13) == Character.digit(cnpj.charAt(13), 10);
	}

	private int checkDigit(String value, int[] weights, int length) {
		int sum = 0;
		for (int index = 0; index < length; index++) {
			sum += Character.digit(value.charAt(index), 10) * weights[index];
		}
		int remainder = sum % 11;
		return remainder < 2 ? 0 : 11 - remainder;
	}
}
