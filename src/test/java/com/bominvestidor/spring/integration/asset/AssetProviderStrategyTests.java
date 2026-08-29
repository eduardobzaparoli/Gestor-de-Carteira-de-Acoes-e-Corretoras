package com.bominvestidor.spring.integration.asset;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;

class AssetProviderStrategyTests {
	@Test
	void brapiFiltersByClassificationAndRepresentsMissingQuoteAsEmpty() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("/api/quote/list?search=PETR&limit=20"))).andRespond(withSuccess("""
			{"stocks":[{"stock":"PETR4","name":"Petrobras PN","subType":"stock"},{"stock":"BOVA11","name":"BOVA11","subType":"etf"},{"stock":"IGNORED","name":"Ignored","subType":"fund"}]}
			""", MediaType.APPLICATION_JSON));
		BrapiAssetSearchStrategy strategy = new BrapiAssetSearchStrategy(builder.build(), brapiProperties());

		var candidates = strategy.findCandidates(AssetType.STOCK, "PETR");
		assertEquals(1, candidates.size());
		assertEquals("PETR4", candidates.get(0).ticker());
		server.verify();

		RestClient.Builder quoteBuilder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer quoteServer = MockRestServiceServer.bindTo(quoteBuilder).build();
		quoteServer.expect(requestTo("http://brapi.test/api/quote/PETR4")).andRespond(withSuccess("{\"results\":[{\"symbol\":\"PETR4\"}]}", MediaType.APPLICATION_JSON));
		assertTrue(new BrapiAssetSearchStrategy(quoteBuilder.build(), brapiProperties()).findQuote("PETR4").isEmpty());
		quoteServer.verify();
	}

	@Test
	void brapiAndAlphaTranslateRateLimitResponses() {
		RestClient.Builder brapiBuilder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer brapiServer = MockRestServiceServer.bindTo(brapiBuilder).build();
		brapiServer.expect(requestTo(containsString("/api/quote/list"))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		AssetProviderUnavailableException brapiError = assertThrows(AssetProviderUnavailableException.class,
				() -> new BrapiAssetSearchStrategy(brapiBuilder.build(), brapiProperties()).findCandidates(AssetType.STOCK, "PETR"));
		assertEquals("BRAPI_RATE_LIMITED", brapiError.getCode());
		brapiServer.verify();

		RestClient.Builder alphaBuilder = RestClient.builder().baseUrl("http://alpha.test");
		MockRestServiceServer alphaServer = MockRestServiceServer.bindTo(alphaBuilder).build();
		alphaServer.expect(requestTo(containsString("function=SYMBOL_SEARCH"))).andRespond(withSuccess("{\"Information\":\"rate limit\"}", MediaType.APPLICATION_JSON));
		AssetProviderUnavailableException alphaError = assertThrows(AssetProviderUnavailableException.class,
				() -> new AlphaVantageAssetSearchStrategy(alphaBuilder.build(), alphaProperties()).findCandidates(AssetType.STOCK, "MSFT"));
		assertEquals("ALPHAVANTAGE_RATE_LIMITED", alphaError.getCode());
		alphaServer.verify();
	}

	@Test
	void alphaFiltersUsEquitiesAndMapsQuote() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://alpha.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("function=SYMBOL_SEARCH"))).andRespond(withSuccess("""
			{"bestMatches":[{"1. symbol":"MSFT","2. name":"Microsoft","3. type":"Equity","4. region":"United States","8. currency":"USD"},{"1. symbol":"BOVA11","2. name":"BOVA","3. type":"ETF","4. region":"Brazil","8. currency":"BRL"}]}
			""", MediaType.APPLICATION_JSON));
		AlphaVantageAssetSearchStrategy strategy = new AlphaVantageAssetSearchStrategy(builder.build(), alphaProperties());
		var candidates = strategy.findCandidates(AssetType.STOCK, "MSFT");
		assertEquals(1, candidates.size());
		assertEquals("MSFT", candidates.get(0).ticker());
		server.verify();

		RestClient.Builder quoteBuilder = RestClient.builder().baseUrl("http://alpha.test");
		MockRestServiceServer quoteServer = MockRestServiceServer.bindTo(quoteBuilder).build();
		quoteServer.expect(requestTo(containsString("function=GLOBAL_QUOTE"))).andRespond(withSuccess("{\"Global Quote\":{\"05. price\":\"123.45\"}}", MediaType.APPLICATION_JSON));
		var quote = new AlphaVantageAssetSearchStrategy(quoteBuilder.build(), alphaProperties()).findQuote("MSFT");
		assertEquals(new BigDecimal("123.45"), quote.orElseThrow().price());
		quoteServer.verify();
	}

	@Test
	void translatesTransportTimeoutIntoPublicProviderFailure() {
		RestClient client = RestClient.builder().requestFactory((uri, method) -> {
			throw new ResourceAccessException("timeout");
		}).build();
		AssetProviderUnavailableException error = assertThrows(AssetProviderUnavailableException.class,
				() -> new AlphaVantageAssetSearchStrategy(client, alphaProperties()).findCandidates(AssetType.STOCK, "MSFT"));
		assertEquals("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", error.getCode());
	}

	private BrokerageIntegrationProperties brapiProperties() { BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setBrapiToken("token"); return properties; }
	private BrokerageIntegrationProperties alphaProperties() { BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setAlphaVantageApiKey("key"); return properties; }
}
