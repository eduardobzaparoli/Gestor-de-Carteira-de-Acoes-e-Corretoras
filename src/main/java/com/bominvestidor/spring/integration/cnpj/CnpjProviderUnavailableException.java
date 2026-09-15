package com.bominvestidor.spring.integration.cnpj;

public class CnpjProviderUnavailableException extends RuntimeException {

	public CnpjProviderUnavailableException() {
		super("CNPJ provider is unavailable");
	}
}
