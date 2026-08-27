package com.bominvestidor.spring.exception;

public class CepNotFoundException extends RuntimeException {

	public CepNotFoundException() {
		super("CEP was not found");
	}
}
