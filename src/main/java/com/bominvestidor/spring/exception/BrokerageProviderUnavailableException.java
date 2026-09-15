package com.bominvestidor.spring.exception;

public class BrokerageProviderUnavailableException extends RuntimeException {

	private final String code;

	public BrokerageProviderUnavailableException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
