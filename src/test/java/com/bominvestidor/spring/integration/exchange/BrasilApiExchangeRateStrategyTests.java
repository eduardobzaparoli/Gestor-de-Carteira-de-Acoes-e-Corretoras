package com.bominvestidor.spring.integration.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.bominvestidor.spring.exception.ExchangeRateUnavailableException;

class BrasilApiExchangeRateStrategyTests {
	@Test
	void selectsThePtaxClosingBuyRate() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil-api.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasil-api.test/api/cambio/v1/cotacao/USD/2026-08-28")).andRespond(withSuccess("""
			{"data":"2026-08-28","cotacoes":[{"cotacao_compra":5.10,"tipo_boletim":"ABERTURA"},{"cotacao_compra":5.25,"tipo_boletim":"FECHAMENTO PTAX"}]}
			""", MediaType.APPLICATION_JSON));

		var rate = new BrasilApiExchangeRateStrategy(builder.build()).findClosingRate("USD", "BRL", LocalDate.of(2026, 8, 28)).orElseThrow();
		assertEquals(0, new BigDecimal("5.25").compareTo(rate.rate()));
		assertEquals(LocalDate.of(2026, 8, 28), rate.referenceDate());
		server.verify();
	}

	@Test
	void looksBackUntilAClosingRateIsAvailable() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil-api.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasil-api.test/api/cambio/v1/cotacao/USD/2026-08-31")).andRespond(withSuccess("""
			{"data":"2026-08-31","cotacoes":[{"cotacao_compra":5.10,"tipo_boletim":"ABERTURA"}]}
			""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("http://brasil-api.test/api/cambio/v1/cotacao/USD/2026-08-30")).andRespond(withSuccess("""
			{"data":"2026-08-29","cotacoes":[{"cotacao_compra":5.20,"tipo_boletim":"FECHAMENTO PTAX"}]}
			""", MediaType.APPLICATION_JSON));

		var rate = new BrasilApiExchangeRateStrategy(builder.build()).findClosingRate("USD", "BRL", LocalDate.of(2026, 8, 31)).orElseThrow();
		assertEquals(LocalDate.of(2026, 8, 29), rate.referenceDate());
		server.verify();
	}

	@Test
	void looksBackWhenTheCurrentDateIsRejectedWhilePtaxIsStillForming() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil-api.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasil-api.test/api/cambio/v1/cotacao/USD/2026-09-03"))
				.andRespond(withBadRequest());
		server.expect(requestTo("http://brasil-api.test/api/cambio/v1/cotacao/USD/2026-09-02")).andRespond(withSuccess("""
			{"data":"2026-09-02","cotacoes":[{"cotacao_compra":5.42,"tipo_boletim":"FECHAMENTO PTAX"}]}
			""", MediaType.APPLICATION_JSON));

		var rate = new BrasilApiExchangeRateStrategy(builder.build())
				.findClosingRate("USD", "BRL", LocalDate.of(2026, 9, 3)).orElseThrow();

		assertEquals(0, new BigDecimal("5.42").compareTo(rate.rate()));
		assertEquals(LocalDate.of(2026, 9, 2), rate.referenceDate());
		server.verify();
	}

	@Test
	void rejectsUnsupportedPairsAndTranslatesProviderFailures() {
		RestClient timeout = RestClient.builder().requestFactory((uri, method) -> { throw new ResourceAccessException("timeout"); }).build();
		assertTrue(new BrasilApiExchangeRateStrategy(timeout).findClosingRate("EUR", "BRL", LocalDate.now()).isEmpty());

		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil-api.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasil-api.test/api/cambio/v1/cotacao/USD/2026-08-28")).andRespond(withServerError());
		assertThrows(ExchangeRateUnavailableException.class,
				() -> new BrasilApiExchangeRateStrategy(builder.build()).findClosingRate("USD", "BRL", LocalDate.of(2026, 8, 28)));
		server.verify();

		assertThrows(ExchangeRateUnavailableException.class,
				() -> new BrasilApiExchangeRateStrategy(timeout).findClosingRate("USD", "BRL", LocalDate.of(2026, 8, 28)));
	}

	@Test
	void representsInvalidProviderDataAsNoAvailableRate() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil-api.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		for (int day = 28; day >= 14; day--) {
			server.expect(requestTo("http://brasil-api.test/api/cambio/v1/cotacao/USD/2026-08-" + day)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		}
		assertTrue(new BrasilApiExchangeRateStrategy(builder.build()).findClosingRate("USD", "BRL", LocalDate.of(2026, 8, 28)).isEmpty());
		server.verify();
	}
}
