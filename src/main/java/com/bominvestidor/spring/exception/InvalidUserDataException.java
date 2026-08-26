package com.bominvestidor.spring.exception;

public class InvalidUserDataException extends RuntimeException {

	private final String field;

	public InvalidUserDataException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
