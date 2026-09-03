package com.bominvestidor.spring.integration.historicalprice;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.exception.HistoricalPriceUnavailableException;

class HistoricalAssetPriceStrategyTests {
	private static final LocalDate START = LocalDate.of(2026, 8, 27);
	private static final LocalDate END = LocalDate.of(2026, 8, 31);

	@Test
	void brapiParsesAndOrdersDailyUnadjustedClosesWithinTheRequestedWindow() {
		long before = Instant.parse("2026-08-26T00:00:00Z").getEpochSecond();
		long first = Instant.parse("2026-08-27T00:00:00Z").getEpochSecond();
		long second = Instant.parse("2026-08-28T00:00:00Z").getEpochSecond();
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("/api/quote/petr4?range=3mo&interval=1d")))
				.andExpect(header("Authorization", "Bearer token"))
				.andRespond(withSuccess("""
					{"results":[{"historicalDataPrice":[
					  {"date":%d,"close":35.20,"adjustedClose":999.00},
					  {"date":%d,"close":35.10,"adjustedClose":888.00},
					  {"date":%d,"close":34.00}
					]}]}
					""".formatted(second, first, before), MediaType.APPLICATION_JSON));

		var series = new BrapiHistoricalAssetPriceStrategy(builder.build(), brapiProperties("token"))
				.findSeries("petr4", "brl", START, END);

		assertEquals(java.util.List.of(START, START.plusDays(1)), series.prices().keySet().stream().toList());
		assertEquals(0, new BigDecimal("35.10").compareTo(series.prices().get(START).close()));
		server.verify();
	}

	@Test
	void alphaParsesAndOrdersDailyUnadjustedClosesWithinTheRequestedWindow() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://alpha.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("function=TIME_SERIES_DAILY")))
				.andRespond(withSuccess("""
					{"Time Series (Daily)":{
					  "2026-08-31":{"4. close":"510.20","5. adjusted close":"999.00"},
					  "2026-08-27":{"4. close":"500.10","5. adjusted close":"888.00"},
					  "2026-08-26":{"4. close":"490.00"}
					}}
					""", MediaType.APPLICATION_JSON));

		var series = new AlphaVantageHistoricalAssetPriceStrategy(builder.build(), alphaProperties("key"))
				.findSeries("msft", "usd", START, END);

		assertEquals(java.util.List.of(START, END), series.prices().keySet().stream().toList());
		assertEquals(0, new BigDecimal("500.10").compareTo(series.prices().get(START).close()));
		server.verify();
	}

	@Test
	void providersTranslateLimitsAndTechnicalFailuresWithoutLeakingPayloads() {
		RestClient.Builder brapiLimitedBuilder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer brapiLimitedServer = MockRestServiceServer.bindTo(brapiLimitedBuilder).build();
		brapiLimitedServer.expect(requestTo(containsString("/api/quote/PETR4"))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		AssetProviderUnavailableException brapiLimit = assertThrows(AssetProviderUnavailableException.class,
				() -> new BrapiHistoricalAssetPriceStrategy(brapiLimitedBuilder.build(), brapiProperties("token"))
						.findSeries("PETR4", "BRL", START, END));
		assertEquals("BRAPI_RATE_LIMITED", brapiLimit.getCode());
		brapiLimitedServer.verify();

		RestClient.Builder alphaLimitedBuilder = RestClient.builder().baseUrl("http://alpha.test");
		MockRestServiceServer alphaLimitedServer = MockRestServiceServer.bindTo(alphaLimitedBuilder).build();
		alphaLimitedServer.expect(requestTo(containsString("function=TIME_SERIES_DAILY")))
				.andRespond(withSuccess("{\"Information\":\"standard API rate limit of 25 requests per day\"}", MediaType.APPLICATION_JSON));
		AssetProviderUnavailableException alphaLimit = assertThrows(AssetProviderUnavailableException.class,
				() -> new AlphaVantageHistoricalAssetPriceStrategy(alphaLimitedBuilder.build(), alphaProperties("key"))
						.findSeries("MSFT", "USD", START, END));
		assertEquals("ALPHAVANTAGE_RATE_LIMITED", alphaLimit.getCode());
		assertEquals("Alpha Vantage rate limit reached", alphaLimit.getMessage());
		alphaLimitedServer.verify();

		RestClient.Builder alphaPlanBuilder = RestClient.builder().baseUrl("http://alpha.test");
		MockRestServiceServer alphaPlanServer = MockRestServiceServer.bindTo(alphaPlanBuilder).build();
		alphaPlanServer.expect(requestTo(containsString("function=TIME_SERIES_DAILY")))
				.andRespond(withSuccess("{\"Information\":\"This endpoint is not available for your subscription\"}", MediaType.APPLICATION_JSON));
		AssetProviderUnavailableException alphaPlan = assertThrows(AssetProviderUnavailableException.class,
				() -> new AlphaVantageHistoricalAssetPriceStrategy(alphaPlanBuilder.build(), alphaProperties("key"))
						.findSeries("MSFT", "USD", START, END));
		assertEquals("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", alphaPlan.getCode());
		alphaPlanServer.verify();

		RestClient timeout = RestClient.builder().requestFactory((uri, method) -> { throw new ResourceAccessException("internal timeout details"); }).build();
		AssetProviderUnavailableException transport = assertThrows(AssetProviderUnavailableException.class,
				() -> new AlphaVantageHistoricalAssetPriceStrategy(timeout, alphaProperties("key"))
						.findSeries("MSFT", "USD", START, END));
		assertEquals("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", transport.getCode());
	}

	@Test
	void providersRejectMissingCredentialsEmptySeriesAndServerFailures() {
		RestClient unused = RestClient.builder().requestFactory((uri, method) -> {
			throw new AssertionError("The provider must not be called without credentials");
		}).build();
		assertEquals("BRAPI_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new BrapiHistoricalAssetPriceStrategy(unused, brapiProperties(" ")).findSeries("PETR4", "BRL", START, END)).getCode());
		assertEquals("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new AlphaVantageHistoricalAssetPriceStrategy(unused, alphaProperties("")).findSeries("MSFT", "USD", START, END)).getCode());

		RestClient.Builder emptyBuilder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(emptyBuilder).build();
		emptyServer.expect(requestTo(containsString("/api/quote/PETR4")))
				.andRespond(withSuccess("{\"results\":[{\"historicalDataPrice\":[]}]}", MediaType.APPLICATION_JSON));
		assertThrows(HistoricalPriceUnavailableException.class,
				() -> new BrapiHistoricalAssetPriceStrategy(emptyBuilder.build(), brapiProperties("token"))
						.findSeries("PETR4", "BRL", START, END));
		emptyServer.verify();

		RestClient.Builder failedBuilder = RestClient.builder().baseUrl("http://alpha.test");
		MockRestServiceServer failedServer = MockRestServiceServer.bindTo(failedBuilder).build();
		failedServer.expect(requestTo(containsString("function=TIME_SERIES_DAILY"))).andRespond(withServerError());
		assertEquals("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new AlphaVantageHistoricalAssetPriceStrategy(failedBuilder.build(), alphaProperties("key"))
						.findSeries("MSFT", "USD", START, END)).getCode());
		failedServer.verify();
	}

	private BrokerageIntegrationProperties brapiProperties(String token) {
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setBrapiToken(token);
		return properties;
	}

	private BrokerageIntegrationProperties alphaProperties(String key) {
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setAlphaVantageApiKey(key);
		return properties;
	}
}
