package com.bominvestidor.spring.exception;

import java.util.List;

import com.bominvestidor.spring.dto.error.FieldErrorResponse;

public class InvalidAssetSearchDataException extends RuntimeException {
	private final List<FieldErrorResponse> fieldErrors;

	public InvalidAssetSearchDataException(List<FieldErrorResponse> fieldErrors) {
		super("Request validation failed");
		this.fieldErrors = List.copyOf(fieldErrors);
	}

	public List<FieldErrorResponse> getFieldErrors() { return fieldErrors; }
}
