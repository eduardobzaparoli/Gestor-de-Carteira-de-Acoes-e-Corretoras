package com.bominvestidor.spring.exception;

public class PortfolioIncomeEventNotFoundException extends RuntimeException {
	public PortfolioIncomeEventNotFoundException() { super("Income event was not found"); }
}
