package com.bominvestidor.spring.exception;

import java.util.List;

import com.bominvestidor.spring.dto.error.FieldErrorResponse;

public class InvalidBrokerageDataException extends RuntimeException {

	private final List<FieldErrorResponse> fieldErrors;

	public InvalidBrokerageDataException(List<FieldErrorResponse> fieldErrors) {
		super("Request validation failed");
		this.fieldErrors = List.copyOf(fieldErrors);
	}

	public InvalidBrokerageDataException(String field, String message) {
		this(List.of(new FieldErrorResponse(field, message)));
	}

	public List<FieldErrorResponse> getFieldErrors() {
		return fieldErrors;
	}
}
