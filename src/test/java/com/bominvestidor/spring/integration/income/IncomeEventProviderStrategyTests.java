package com.bominvestidor.spring.integration.income;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.http.HttpStatus;
import java.util.Set;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.income.IncomeEventType;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;

class IncomeEventProviderStrategyTests {
	@Test void batchesBrapiTickersAndKeepsRequestedTickerAfterRename() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test"); MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setBrapiToken("token");
		server.expect(requestTo("http://brapi.test/api/v2/stocks/dividends?symbols=PETR4,VALE3")).andRespond(withSuccess("""
			{"results":[{"requestedSymbol":"PETR4","symbol":"PETR4","data":{"cashDividends":[]}},{"requestedSymbol":"VALE3","symbol":"VALE3","data":{"cashDividends":[{"rate":2,"lastDatePrior":"2026-01-10 00:00:00+00","paymentDate":"2026-01-20 00:00:00+00","label":"DIVIDENDO"}]}}]}
			""", MediaType.APPLICATION_JSON));
		var result = new BrapiIncomeEventProviderStrategy(builder.build(), properties).findEvents(Set.of("VALE3", "PETR4"));
		assertThat(result.events()).containsOnlyKeys("PETR4", "VALE3");
		assertThat(result.events().get("PETR4")).isEmpty();
		assertThat(result.events().get("VALE3")).singleElement().satisfies(event -> assertThat(event.unitAmount()).isEqualByComparingTo("2"));
		server.verify();
	}
	@Test void normalizesBrapiCashEventsAndIgnoresInvalidEntries() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brapi.test"); MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setBrapiToken("token");
		server.expect(requestTo("http://brapi.test/api/v2/stocks/dividends?symbols=PETR4")).andRespond(withSuccess("""
			{"results":[{"requestedSymbol":"PETR4","symbol":"PETR4","changed":false,"data":{"cashDividends":[{"rate":1.2,"lastDatePrior":"2026-01-10","paymentDate":"2026-01-20","label":"JCP"},{"rate":0,"lastDatePrior":"2026-01-10","paymentDate":"2026-01-20"}]}}]}
			""", MediaType.APPLICATION_JSON));
		var events = new BrapiIncomeEventProviderStrategy(builder.build(), properties).findEvents("PETR4");
		assertThat(events).singleElement().satisfies(event -> { assertThat(event.type()).isEqualTo(IncomeEventType.INTEREST_ON_EQUITY); assertThat(event.eventKey()).contains("PETR4"); }); server.verify();
	}
	@Test void normalizesAlphaDividendsAndExposesRateLimit() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://alpha.test"); MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setAlphaVantageApiKey("key");
		server.expect(requestTo("http://alpha.test/query?function=DIVIDENDS&symbol=MSFT&apikey=key")).andRespond(withSuccess("""
			{"data":[{"ex_dividend_date":"2026-01-10","payment_date":"2026-02-10","amount":"0.83"},{"ex_dividend_date":"","payment_date":"2026-02-10","amount":"1"}]}
			""", MediaType.APPLICATION_JSON));
		var events = new AlphaVantageIncomeEventProviderStrategy(builder.build(), properties).findEvents("MSFT");
		assertThat(events).singleElement().satisfies(event -> assertThat(event.unitAmount()).isEqualByComparingTo("0.83")); server.verify();
		RestClient.Builder rateBuilder = RestClient.builder().baseUrl("http://alpha.test"); MockRestServiceServer rateServer = MockRestServiceServer.bindTo(rateBuilder).build();
		rateServer.expect(requestTo("http://alpha.test/query?function=DIVIDENDS&symbol=MSFT&apikey=key")).andRespond(withSuccess("{\"Note\":\"limit\"}", MediaType.APPLICATION_JSON));
		assertThatThrownBy(() -> new AlphaVantageIncomeEventProviderStrategy(rateBuilder.build(), properties).findEvents("MSFT"))
			.isInstanceOf(AssetProviderUnavailableException.class).hasMessageContaining("rate limit"); rateServer.verify();
	}
	@Test void translatesBrapiLimitsTechnicalFailuresAndMissingAccess() {
		BrokerageIntegrationProperties missing = new BrokerageIntegrationProperties();
		assertThatThrownBy(() -> new BrapiIncomeEventProviderStrategy(failingClient(), missing).findEvents("PETR4"))
			.isInstanceOf(AssetProviderUnavailableException.class).hasMessageContaining("authentication");
		RestClient.Builder limitedBuilder = RestClient.builder().baseUrl("http://brapi.test"); MockRestServiceServer limitedServer = MockRestServiceServer.bindTo(limitedBuilder).build();
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties(); properties.setBrapiToken("token");
		limitedServer.expect(requestTo("http://brapi.test/api/v2/stocks/dividends?symbols=PETR4")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		assertThatThrownBy(() -> new BrapiIncomeEventProviderStrategy(limitedBuilder.build(), properties).findEvents("PETR4"))
			.isInstanceOf(AssetProviderUnavailableException.class).hasMessageContaining("rate limit"); limitedServer.verify();
		RestClient failing = RestClient.builder().requestFactory((uri, method) -> { throw new ResourceAccessException("timeout"); }).build();
		assertThatThrownBy(() -> new BrapiIncomeEventProviderStrategy(failing, properties).findEvents("PETR4"))
			.isInstanceOf(AssetProviderUnavailableException.class).hasMessageContaining("unavailable");
	}
	@Test void returnsEmptyCollectionsForMissingProviderFields() {
		RestClient.Builder brapiBuilder = RestClient.builder().baseUrl("http://brapi.test"); MockRestServiceServer brapiServer = MockRestServiceServer.bindTo(brapiBuilder).build();
		BrokerageIntegrationProperties brapiProperties = new BrokerageIntegrationProperties(); brapiProperties.setBrapiToken("token");
		brapiServer.expect(requestTo("http://brapi.test/api/v2/stocks/dividends?symbols=PETR4")).andRespond(withSuccess("{\"results\":[{\"requestedSymbol\":\"PETR4\",\"data\":{}}]}", MediaType.APPLICATION_JSON));
		assertThat(new BrapiIncomeEventProviderStrategy(brapiBuilder.build(), brapiProperties).findEvents("PETR4")).isEmpty(); brapiServer.verify();
		BrokerageIntegrationProperties alphaMissing = new BrokerageIntegrationProperties();
		assertThatThrownBy(() -> new AlphaVantageIncomeEventProviderStrategy(failingClient(), alphaMissing).findEvents("MSFT"))
			.isInstanceOf(AssetProviderUnavailableException.class).hasMessageContaining("authentication");
	}
	private RestClient failingClient() {
		return RestClient.builder().requestFactory((uri, method) -> { throw new ResourceAccessException("blocked"); }).build();
	}
}
