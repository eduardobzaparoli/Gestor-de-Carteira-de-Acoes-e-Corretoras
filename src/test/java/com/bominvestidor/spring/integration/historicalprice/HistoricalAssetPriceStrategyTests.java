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
	void twelveDataParsesAndOrdersDailyUnadjustedClosesWithinTheRequestedWindow() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(containsString("/time_series?symbol=msft&interval=1day&start_date=2026-08-27&end_date=2026-08-31&adjust=none")))
				.andRespond(withSuccess("""
					{"values":[
					  {"datetime":"2026-08-31","close":"510.20"},
					  {"datetime":"2026-08-27","close":"500.10"},
					  {"datetime":"2026-08-26","close":"490.00"},
					  {"datetime":"invalid","close":"999.00"}
					]}
					""", MediaType.APPLICATION_JSON));

		var series = new TwelveDataHistoricalAssetPriceStrategy(builder.build(), twelveProperties("key"))
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

		RestClient.Builder twelveLimitedBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer twelveLimitedServer = MockRestServiceServer.bindTo(twelveLimitedBuilder).build();
		twelveLimitedServer.expect(requestTo(containsString("/time_series")))
				.andRespond(withSuccess("{\"status\":\"error\",\"code\":429,\"message\":\"Run out of API credits\"}", MediaType.APPLICATION_JSON));
		AssetProviderUnavailableException twelveLimit = assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataHistoricalAssetPriceStrategy(twelveLimitedBuilder.build(), twelveProperties("key"))
						.findSeries("MSFT", "USD", START, END));
		assertEquals("TWELVE_DATA_RATE_LIMITED", twelveLimit.getCode());
		assertEquals("Twelve Data rate limit reached", twelveLimit.getMessage());
		twelveLimitedServer.verify();

		RestClient.Builder twelvePlanBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer twelvePlanServer = MockRestServiceServer.bindTo(twelvePlanBuilder).build();
		twelvePlanServer.expect(requestTo(containsString("/time_series")))
				.andRespond(withSuccess("{\"status\":\"error\",\"code\":401,\"message\":\"API key is invalid\"}", MediaType.APPLICATION_JSON));
		AssetProviderUnavailableException twelvePlan = assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataHistoricalAssetPriceStrategy(twelvePlanBuilder.build(), twelveProperties("key"))
						.findSeries("MSFT", "USD", START, END));
		assertEquals("TWELVE_DATA_PROVIDER_UNAVAILABLE", twelvePlan.getCode());
		twelvePlanServer.verify();

		RestClient timeout = RestClient.builder().requestFactory((uri, method) -> { throw new ResourceAccessException("internal timeout details"); }).build();
		AssetProviderUnavailableException transport = assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataHistoricalAssetPriceStrategy(timeout, twelveProperties("key"))
						.findSeries("MSFT", "USD", START, END));
		assertEquals("TWELVE_DATA_PROVIDER_UNAVAILABLE", transport.getCode());
	}

	@Test
	void providersRejectMissingCredentialsEmptySeriesAndServerFailures() {
		RestClient unused = RestClient.builder().requestFactory((uri, method) -> {
			throw new AssertionError("The provider must not be called without credentials");
		}).build();
		assertEquals("BRAPI_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new BrapiHistoricalAssetPriceStrategy(unused, brapiProperties(" ")).findSeries("PETR4", "BRL", START, END)).getCode());
		assertEquals("TWELVE_DATA_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataHistoricalAssetPriceStrategy(unused, twelveProperties("")).findSeries("MSFT", "USD", START, END)).getCode());

		RestClient.Builder emptyBuilder = RestClient.builder().baseUrl("http://brapi.test");
		MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(emptyBuilder).build();
		emptyServer.expect(requestTo(containsString("/api/quote/PETR4")))
				.andRespond(withSuccess("{\"results\":[{\"historicalDataPrice\":[]}]}", MediaType.APPLICATION_JSON));
		assertThrows(HistoricalPriceUnavailableException.class,
				() -> new BrapiHistoricalAssetPriceStrategy(emptyBuilder.build(), brapiProperties("token"))
						.findSeries("PETR4", "BRL", START, END));
		emptyServer.verify();

		RestClient.Builder twelveEmptyBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer twelveEmptyServer = MockRestServiceServer.bindTo(twelveEmptyBuilder).build();
		twelveEmptyServer.expect(requestTo(containsString("/time_series")))
				.andRespond(withSuccess("{\"values\":[]}", MediaType.APPLICATION_JSON));
		assertThrows(HistoricalPriceUnavailableException.class,
				() -> new TwelveDataHistoricalAssetPriceStrategy(twelveEmptyBuilder.build(), twelveProperties("key"))
						.findSeries("MSFT", "USD", START, END));
		twelveEmptyServer.verify();

		RestClient.Builder failedBuilder = RestClient.builder().baseUrl("http://twelve.test");
		MockRestServiceServer failedServer = MockRestServiceServer.bindTo(failedBuilder).build();
		failedServer.expect(requestTo(containsString("/time_series"))).andRespond(withServerError());
		assertEquals("TWELVE_DATA_PROVIDER_UNAVAILABLE", assertThrows(AssetProviderUnavailableException.class,
				() -> new TwelveDataHistoricalAssetPriceStrategy(failedBuilder.build(), twelveProperties("key"))
						.findSeries("MSFT", "USD", START, END)).getCode());
		failedServer.verify();
	}

	private BrokerageIntegrationProperties brapiProperties(String token) {
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setBrapiToken(token);
		return properties;
	}

	private BrokerageIntegrationProperties twelveProperties(String key) {
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setTwelveDataApiKey(key);
		return properties;
	}
}
