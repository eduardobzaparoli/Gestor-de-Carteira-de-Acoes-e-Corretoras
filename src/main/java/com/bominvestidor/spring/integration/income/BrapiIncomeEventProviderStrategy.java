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

@Component
public class BrapiIncomeEventProviderStrategy implements IncomeEventProviderStrategy {
	private final RestClient client;
	private final String token;

	public BrapiIncomeEventProviderStrategy(@Qualifier("brapiRestClient") RestClient client,
			BrokerageIntegrationProperties properties) { this.client = client; this.token = properties.getBrapiToken(); }
	@Override public AssetMarket market() { return AssetMarket.BR; }

	@Override
	public List<IncomeProviderEvent> findEvents(String ticker) {
		try {
			BrapiIncomeResponse response = request(ticker).retrieve().body(BrapiIncomeResponse.class);
			if (response == null || response.results() == null || response.results().isEmpty()) return List.of();
			BrapiIncomeQuote quote = response.results().get(0);
			if (quote.dividendsData() == null || quote.dividendsData().cashDividends() == null) return List.of();
			return quote.dividendsData().cashDividends().stream().map(item -> toEvent(ticker, item)).flatMap(java.util.Optional::stream).toList();
		} catch (RestClientResponseException exception) { throw translate(exception); }
		catch (RestClientException exception) { throw unavailable(); }
	}

	private RestClient.RequestHeadersSpec<?> request(String ticker) {
		if (token == null || token.isBlank()) throw unavailable();
		return client.get().uri("/api/quote/{ticker}?dividends=true", ticker).header("Authorization", "Bearer " + token);
	}
	private java.util.Optional<IncomeProviderEvent> toEvent(String ticker, BrapiCashDividend item) {
		try {
			if (item == null || item.rate() == null || item.rate().signum() <= 0 || blank(item.lastDatePrior()) || blank(item.paymentDate())) return java.util.Optional.empty();
			LocalDate eligibility = LocalDate.parse(item.lastDatePrior());
			LocalDate payment = LocalDate.parse(item.paymentDate());
			String label = item.label() == null ? "" : item.label().toUpperCase();
			IncomeEventType type = label.contains("JCP") || label.contains("JUROS") ? IncomeEventType.INTEREST_ON_EQUITY
					: label.contains("RENDIMENTO") || label.contains("DISTRIBUI") ? IncomeEventType.DISTRIBUTION : IncomeEventType.DIVIDEND;
			String key = "brapi|" + ticker.toUpperCase() + "|" + type + "|" + eligibility + "|" + payment + "|" + item.rate().stripTrailingZeros().toPlainString();
			return java.util.Optional.of(new IncomeProviderEvent(key, type, item.rate(), eligibility, payment, IncomeEventSource.BRAPI));
		} catch (RuntimeException exception) { return java.util.Optional.empty(); }
	}
	private AssetProviderUnavailableException translate(RestClientResponseException exception) { return exception.getStatusCode().value() == 429 ? new AssetProviderUnavailableException("BRAPI_RATE_LIMITED", "Brapi rate limit reached") : unavailable(); }
	private AssetProviderUnavailableException unavailable() { return new AssetProviderUnavailableException("BRAPI_PROVIDER_UNAVAILABLE", "Brapi is unavailable"); }
	private boolean blank(String value) { return value == null || value.isBlank(); }
	private record BrapiIncomeResponse(List<BrapiIncomeQuote> results) { }
	private record BrapiIncomeQuote(BrapiDividendsData dividendsData) { }
	private record BrapiDividendsData(List<BrapiCashDividend> cashDividends) { }
	private record BrapiCashDividend(BigDecimal rate, String lastDatePrior, String paymentDate, String label) { }
}
