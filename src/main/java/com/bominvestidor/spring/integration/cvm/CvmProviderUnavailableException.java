package com.bominvestidor.spring.integration.cvm;

public class CvmProviderUnavailableException extends RuntimeException {

	public CvmProviderUnavailableException() {
		super("CVM provider is unavailable");
	}
}
