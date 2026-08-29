package com.bominvestidor.spring.integration.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

class AlphaVantageResponseTests {

	@Test
 	void mapsProviderLimitFieldsWithTheirOriginalCapitalization() {
		var searchInformation = AlphaVantageAssetSearchStrategy.AlphaSearchResponse.class.getDeclaredConstructors()[0]
				.getParameters()[2].getAnnotation(JsonProperty.class);
		var quoteNote = AlphaVantageAssetSearchStrategy.AlphaQuoteResponse.class.getDeclaredConstructors()[0]
				.getParameters()[1].getAnnotation(JsonProperty.class);

		assertEquals("Information", searchInformation.value());
		assertEquals("Note", quoteNote.value());
	}
}
