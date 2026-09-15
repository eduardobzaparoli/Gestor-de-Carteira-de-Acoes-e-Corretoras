package com.bominvestidor.spring.exception;

public class RegisteredAssetConflictException extends RuntimeException {
	private final String code;

	public RegisteredAssetConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String getCode() { return code; }
}
