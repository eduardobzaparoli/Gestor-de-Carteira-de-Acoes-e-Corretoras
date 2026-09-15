package com.bominvestidor.spring.exception;
public class PortfolioTransactionConflictException extends RuntimeException {
	private final String code;
	public PortfolioTransactionConflictException(String code, String message) { super(message); this.code = code; }
	public String getCode() { return code; }
}
