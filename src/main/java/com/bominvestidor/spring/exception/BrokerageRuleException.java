package com.bominvestidor.spring.exception;

public class BrokerageRuleException extends RuntimeException {

	private final String code;

	public BrokerageRuleException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
