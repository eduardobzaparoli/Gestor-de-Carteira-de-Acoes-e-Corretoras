package com.bominvestidor.spring.exception;

public class AccountInactiveException extends RuntimeException {

	public AccountInactiveException() {
		super("Account is inactive");
	}
}
