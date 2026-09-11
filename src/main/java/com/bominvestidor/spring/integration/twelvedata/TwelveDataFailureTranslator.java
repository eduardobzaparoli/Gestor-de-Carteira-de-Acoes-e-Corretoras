package com.bominvestidor.spring.integration.twelvedata;

import java.util.Locale;

import org.springframework.web.client.RestClientResponseException;

import com.bominvestidor.spring.exception.AssetProviderUnavailableException;

public final class TwelveDataFailureTranslator {
	private TwelveDataFailureTranslator() { }

	public static void throwIfError(String status, Integer code, String message) {
		if ("error".equalsIgnoreCase(status) || code != null) throw translate(code, message);
	}

	public static AssetProviderUnavailableException translate(RestClientResponseException exception) {
		return exception.getStatusCode().value() == 429 ? rateLimited() : unavailable();
	}

	public static AssetProviderUnavailableException translate(Integer code, String message) {
		return (code != null && code == 429) || mentionsLimit(message) ? rateLimited() : unavailable();
	}

	public static AssetProviderUnavailableException rateLimited() {
		return new AssetProviderUnavailableException("TWELVE_DATA_RATE_LIMITED", "Twelve Data rate limit reached");
	}

	public static AssetProviderUnavailableException unavailable() {
		return new AssetProviderUnavailableException("TWELVE_DATA_PROVIDER_UNAVAILABLE", "Twelve Data is unavailable");
	}

	private static boolean mentionsLimit(String message) {
		if (message == null) return false;
		String normalized = message.toLowerCase(Locale.ROOT);
		return normalized.contains("rate limit") || normalized.contains("too many requests")
				|| normalized.contains("run out of api credits") || normalized.contains("credit limit");
	}
}
