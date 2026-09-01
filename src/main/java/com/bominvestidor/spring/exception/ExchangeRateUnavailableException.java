package com.bominvestidor.spring.exception;

public class ExchangeRateUnavailableException extends RuntimeException {
	public ExchangeRateUnavailableException() { super("Exchange rate is unavailable"); }
}
