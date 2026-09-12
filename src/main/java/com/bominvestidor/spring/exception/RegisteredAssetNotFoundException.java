package com.bominvestidor.spring.exception;

public class RegisteredAssetNotFoundException extends RuntimeException {
	public RegisteredAssetNotFoundException() {
		super("Registered asset was not found");
	}
}
