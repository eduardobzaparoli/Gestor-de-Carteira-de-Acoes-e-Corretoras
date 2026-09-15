package com.bominvestidor.spring.exception;

public class BrokerageNotFoundException extends RuntimeException {

	public BrokerageNotFoundException() {
		super("Brokerage was not found");
	}
}
