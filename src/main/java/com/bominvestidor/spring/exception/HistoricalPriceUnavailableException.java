package com.bominvestidor.spring.exception;

public class HistoricalPriceUnavailableException extends RuntimeException {
	public HistoricalPriceUnavailableException() { super("Historical market prices are unavailable"); }
}
