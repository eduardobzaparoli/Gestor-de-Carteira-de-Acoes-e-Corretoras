package com.bominvestidor.spring.exception;

public class PortfolioNotFoundException extends RuntimeException {

	public PortfolioNotFoundException() {
		super("Portfolio was not found");
	}
}
