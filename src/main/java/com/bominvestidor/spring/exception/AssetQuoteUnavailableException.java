package com.bominvestidor.spring.exception;

public class AssetQuoteUnavailableException extends RuntimeException {
	public AssetQuoteUnavailableException() { super("Asset quote is unavailable"); }
}
