package com.bominvestidor.spring.exception;

public class BrokerageConflictException extends RuntimeException {

	private final String code;

	public BrokerageConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
