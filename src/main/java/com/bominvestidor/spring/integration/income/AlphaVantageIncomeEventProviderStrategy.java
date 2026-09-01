package com.bominvestidor.spring.integration.income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventType;
import com.bominvestidor.spring.domain.income.IncomeProviderEvent;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class AlphaVantageIncomeEventProviderStrategy implements IncomeEventProviderStrategy {
	private final RestClient client;
	private final String apiKey;
	public AlphaVantageIncomeEventProviderStrategy(@Qualifier("alphaVantageRestClient") RestClient client,
			BrokerageIntegrationProperties properties) { this.client = client; this.apiKey = properties.getAlphaVantageApiKey(); }
	@Override public AssetMarket market() { return AssetMarket.US; }

	@Override
	public List<IncomeProviderEvent> findEvents(String ticker) {
		try {
			AlphaDividendResponse response = client.get().uri(uri -> uri.path("/query").queryParam("function", "DIVIDENDS")
					.queryParam("symbol", ticker).queryParam("apikey", requiredApiKey()).build()).retrieve().body(AlphaDividendResponse.class);
			if (response == null) throw unavailable();
			if (response.note() != null || response.information() != null) throw rateLimited();
			return response.data() == null ? List.of() : response.data().stream().map(item -> toEvent(ticker, item)).flatMap(java.util.Optional::stream).toList();
		} catch (RestClientResponseException exception) { throw translate(exception); }
		catch (RestClientException exception) { throw unavailable(); }
	}

	private java.util.Optional<IncomeProviderEvent> toEvent(String ticker, AlphaDividend item) {
		try {
			BigDecimal rate = new BigDecimal(item.amount());
			if (rate.signum() <= 0 || blank(item.exDividendDate()) || blank(item.paymentDate())) return java.util.Optional.empty();
			LocalDate eligibility = LocalDate.parse(item.exDividendDate());
			LocalDate payment = LocalDate.parse(item.paymentDate());
			String key = "alphavantage|" + ticker.toUpperCase() + "|" + eligibility + "|" + payment + "|" + rate.stripTrailingZeros().toPlainString();
			return java.util.Optional.of(new IncomeProviderEvent(key, IncomeEventType.DIVIDEND, rate, eligibility, payment, IncomeEventSource.ALPHA_VANTAGE));
		} catch (RuntimeException exception) { return java.util.Optional.empty(); }
	}
	private String requiredApiKey() { if (apiKey == null || apiKey.isBlank()) throw unavailable(); return apiKey; }
	private AssetProviderUnavailableException translate(RestClientResponseException exception) { return exception.getStatusCode().value() == 429 ? rateLimited() : unavailable(); }
	private AssetProviderUnavailableException rateLimited() { return new AssetProviderUnavailableException("ALPHAVANTAGE_RATE_LIMITED", "Alpha Vantage rate limit reached"); }
	private AssetProviderUnavailableException unavailable() { return new AssetProviderUnavailableException("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", "Alpha Vantage is unavailable"); }
	private boolean blank(String value) { return value == null || value.isBlank(); }
	private record AlphaDividendResponse(List<AlphaDividend> data, @JsonProperty("Note") String note, @JsonProperty("Information") String information) { }
	private record AlphaDividend(@JsonProperty("ex_dividend_date") String exDividendDate, @JsonProperty("payment_date") String paymentDate, String amount) { }
}
