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
	void brapiAndTwelveDataTranslateRateLimitResponses() {
		RestClient.Builder brapiBuilder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer brapiServer = MockRestServiceServer.bindTo(brapiBuilder).build();
		brapiServer.expect(requestTo(containsString("/api/quote/list"))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		AssetProviderUnavailableException brapiError = assertThrows(AssetProviderUnavailableException.class,
				() -> new BrapiAssetSearchStrategy(brapiBuilder.build(), brapiProperties()).findCandidates(AssetType.STOCK, "PETR"));
		assertEquals("BRAPI_RATE_LIMITED", brapiError.getCode());
		brapiServer.verify();

		RestClient.Builder twelveBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer twelveServer = MockRestServiceServer.bindTo(twelveBuilder).build();
		twelveServer.expect(requestTo(containsString("/symbol_search"))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		AssetProviderUnavailableException twelveError = assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataAssetSearchStrategy(twelveBuilder.build(), twelveProperties()).findCandidates(AssetType.STOCK, "MSFT"));
		assertEquals("TWELVE_DATA_RATE_LIMITED", twelveError.getCode());
		twelveServer.verify();
	}

	@Test
	void twelveDataFiltersUsStocksAndMapsQuote() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("/symbol_search?symbol=MSFT"))).andRespond(withSuccess("""
			{"data":[{"symbol":"MSFT","instrument_name":"Microsoft","instrument_type":"Common Stock","country":"United States","currency":"USD"},{"symbol":"SPY","instrument_name":"SPDR","instrument_type":"ETF","country":"United States","currency":"USD"},{"symbol":"BOVA11","instrument_name":"BOVA","instrument_type":"ETF","country":"Brazil","currency":"BRL"}]}
			""", MediaType.APPLICATION_JSON));
		TwelveDataAssetSearchStrategy strategy = new TwelveDataAssetSearchStrategy(builder.build(), twelveProperties());
		var candidates = strategy.findCandidates(AssetType.STOCK, "MSFT");
		assertEquals(1, candidates.size());
		assertEquals("MSFT", candidates.get(0).ticker());
		server.verify();

		RestClient.Builder quoteBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer quoteServer = MockRestServiceServer.bindTo(quoteBuilder).build();
		quoteServer.expect(requestTo(containsString("/price?symbol=MSFT"))).andRespond(withSuccess("{\"price\":\"123.45\"}", MediaType.APPLICATION_JSON));
		var quote = new TwelveDataAssetSearchStrategy(quoteBuilder.build(), twelveProperties()).findQuote("MSFT");
		assertEquals(new BigDecimal("123.45"), quote.orElseThrow().price());
		quoteServer.verify();
	}

	@Test
	void twelveDataDeduplicatesCandidatesByNormalizedTicker() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("/symbol_search?symbol=MCFT"))).andRespond(withSuccess("""
			{"data":[
			  {"symbol":"mcft","instrument_name":"MasterCraft Boat Holdings, Inc.","instrument_type":"Common Stock","country":"United States","currency":"USD"},
			  {"symbol":"MCFT","instrument_name":"MasterCraft Boat Holdings, Inc.","instrument_type":"Common Stock","country":"US","currency":"USD"}
			]}
			""", MediaType.APPLICATION_JSON));

		var candidates = new TwelveDataAssetSearchStrategy(builder.build(), twelveProperties())
				.findCandidates(AssetType.STOCK, "MCFT");

		assertEquals(1, candidates.size());
		assertEquals("MCFT", candidates.get(0).ticker());
		server.verify();
	}

	@Test
	void twelveDataClassifiesEtfsAndTranslatesBodyErrors() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("/symbol_search"))).andRespond(withSuccess("""
			{"data":[{"symbol":"SPY","instrument_name":"SPDR S&P 500 ETF Trust","instrument_type":"ETF","country":"US","currency":"USD"}]}
			""", MediaType.APPLICATION_JSON));
		var candidates = new TwelveDataAssetSearchStrategy(builder.build(), twelveProperties()).findCandidates(AssetType.ETF, "SPY");
		assertEquals(1, candidates.size());
		assertEquals(AssetType.ETF, candidates.get(0).assetType());
		server.verify();

		RestClient.Builder limitedBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer limitedServer = MockRestServiceServer.bindTo(limitedBuilder).build();
		limitedServer.expect(requestTo(containsString("/price"))).andRespond(withSuccess(
				"{\"status\":\"error\",\"code\":429,\"message\":\"Run out of API credits\"}", MediaType.APPLICATION_JSON));
		AssetProviderUnavailableException limited = assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataAssetSearchStrategy(limitedBuilder.build(), twelveProperties()).findQuote("SPY"));
		assertEquals("TWELVE_DATA_RATE_LIMITED", limited.getCode());
		limitedServer.verify();
	}

	@Test
	void twelveDataRepresentsEmptySearchAndNonPositiveQuoteWithoutInventingData() {
		RestClient.Builder searchBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer searchServer = MockRestServiceServer.bindTo(searchBuilder).build();
		searchServer.expect(requestTo(containsString("/symbol_search")))
				.andRespond(withSuccess("{\"data\":[]}", MediaType.APPLICATION_JSON));
		assertTrue(new TwelveDataAssetSearchStrategy(searchBuilder.build(), twelveProperties())
				.findCandidates(AssetType.STOCK, "UNKNOWN").isEmpty());
		searchServer.verify();

		RestClient.Builder quoteBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer quoteServer = MockRestServiceServer.bindTo(quoteBuilder).build();
		quoteServer.expect(requestTo(containsString("/price")))
				.andRespond(withSuccess("{\"price\":\"0\"}", MediaType.APPLICATION_JSON));
		assertTrue(new TwelveDataAssetSearchStrategy(quoteBuilder.build(), twelveProperties())
				.findQuote("UNKNOWN").isEmpty());
		quoteServer.verify();
	}

	@Test
	void translatesTransportTimeoutIntoPublicProviderFailure() {
		RestClient client = RestClient.builder().requestFactory((uri, method) -> {
			throw new ResourceAccessException("timeout");
		}).build();
		AssetProviderUnavailableException error = assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataAssetSearchStrategy(client, twelveProperties()).findCandidates(AssetType.STOCK, "MSFT"));
		assertEquals("TWELVE_DATA_PROVIDER_UNAVAILABLE", error.getCode());
	}

	@Test
	void twelveDataRejectsMissingCredentialAndMalformedPriceWithoutCallingFallback() {
		RestClient unused = RestClient.builder().requestFactory((uri, method) -> {
			throw new AssertionError("Twelve Data must not be called without credentials");
		}).build();
		BrokerageIntegrationProperties missing = new BrokerageIntegrationProperties();
		assertEquals("TWELVE_DATA_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataAssetSearchStrategy(unused, missing).findQuote("MSFT")).getCode());

		RestClient.Builder builder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("/price"))).andRespond(withSuccess("{\"price\":\"not-a-number\"}", MediaType.APPLICATION_JSON));
		assertEquals("TWELVE_DATA_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataAssetSearchStrategy(builder.build(), twelveProperties()).findQuote("MSFT")).getCode());
		server.verify();
	}

	private BrokerageIntegrationProperties brapiProperties() { BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setBrapiToken("token"); return properties; }
	private BrokerageIntegrationProperties twelveProperties() { BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setTwelveDataApiKey("key"); return properties; }
}
