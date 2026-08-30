package com.bominvestidor.spring.exception;
public class PortfolioTransactionNotFoundException extends RuntimeException {
	public PortfolioTransactionNotFoundException() { super("Portfolio transaction was not found"); }
}
