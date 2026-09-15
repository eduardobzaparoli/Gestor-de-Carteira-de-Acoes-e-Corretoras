package com.bominvestidor.spring.exception;

public class PortfolioConflictException extends RuntimeException {

	private final String code;

	public PortfolioConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
