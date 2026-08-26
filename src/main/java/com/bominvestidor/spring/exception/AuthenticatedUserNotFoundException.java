package com.bominvestidor.spring.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {

	public AuthenticatedUserNotFoundException() {
		super("Authenticated user is no longer available");
	}
}
