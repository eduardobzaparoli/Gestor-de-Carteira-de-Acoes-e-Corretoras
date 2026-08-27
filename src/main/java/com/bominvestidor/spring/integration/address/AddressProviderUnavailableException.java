package com.bominvestidor.spring.integration.address;

public class AddressProviderUnavailableException extends RuntimeException {

	public AddressProviderUnavailableException() {
		super("Address provider is unavailable");
	}
}
