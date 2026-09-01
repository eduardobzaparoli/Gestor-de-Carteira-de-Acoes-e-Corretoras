package com.bominvestidor.spring.exception;

import java.util.List;

import com.bominvestidor.spring.dto.error.FieldErrorResponse;

public class InvalidIncomeEventDataException extends RuntimeException {
	private final List<FieldErrorResponse> fieldErrors;
	public InvalidIncomeEventDataException(List<FieldErrorResponse> fieldErrors) { this.fieldErrors = List.copyOf(fieldErrors); }
	public List<FieldErrorResponse> getFieldErrors() { return fieldErrors; }
}
