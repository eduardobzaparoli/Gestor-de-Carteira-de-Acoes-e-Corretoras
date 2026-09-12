package com.bominvestidor.spring.integration.income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
		String normalized = ticker.trim().toUpperCase(Locale.ROOT);
		IncomeProviderBatchResult result = findEvents(Set.of(normalized));
		AssetProviderUnavailableException failure = result.failures().get(normalized);
		if (failure != null) throw failure;
		return result.events().getOrDefault(normalized, List.of());
	}

	@Override
	public IncomeProviderBatchResult findEvents(Set<String> tickers) {
		Map<String, List<IncomeProviderEvent>> events = new LinkedHashMap<>();
		Set<String> normalized = tickers.stream().map(value -> value.trim().toUpperCase(Locale.ROOT))
				.collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new));
		if (normalized.isEmpty()) return new IncomeProviderBatchResult(events, Map.of());
		try {
			BrapiIncomeResponse response = request(String.join(",", normalized)).retrieve().body(BrapiIncomeResponse.class);
			if (response == null || response.results() == null) throw invalidResponse();
			for (String ticker : normalized) events.put(ticker, List.of());
			for (BrapiIncomeSeries series : response.results()) {
				if (series == null) continue;
				String requested = blank(series.requestedSymbol()) ? series.symbol() : series.requestedSymbol();
				if (blank(requested)) continue;
				String ticker = requested.trim().toUpperCase(Locale.ROOT);
				if (!normalized.contains(ticker)) continue;
				List<BrapiCashDividend> cash = series.data() == null || series.data().cashDividends() == null
						? List.of() : series.data().cashDividends();
				events.put(ticker, cash.stream().map(item -> toEvent(ticker, item))
						.flatMap(java.util.Optional::stream).toList());
			}
			return new IncomeProviderBatchResult(events, Map.of());
		} catch (RestClientResponseException exception) { throw translate(exception); }
		catch (RestClientException exception) { throw unavailable(); }
	}

	private RestClient.RequestHeadersSpec<?> request(String symbols) {
		if (token == null || token.isBlank()) throw authenticationFailed();
		return client.get().uri(uri -> uri.path("/api/v2/stocks/dividends").queryParam("symbols", symbols).build())
				.header("Authorization", "Bearer " + token);
	}
	private java.util.Optional<IncomeProviderEvent> toEvent(String ticker, BrapiCashDividend item) {
		try {
			if (item == null || item.rate() == null || item.rate().signum() <= 0 || blank(item.lastDatePrior()) || blank(item.paymentDate())) return java.util.Optional.empty();
			LocalDate eligibility = date(item.lastDatePrior());
			LocalDate payment = date(item.paymentDate());
			String label = item.label() == null ? "" : item.label().toUpperCase(Locale.ROOT);
			IncomeEventType type = label.contains("JCP") || label.contains("JUROS") ? IncomeEventType.INTEREST_ON_EQUITY
					: label.contains("RENDIMENTO") || label.contains("DISTRIBUI") ? IncomeEventType.DISTRIBUTION : IncomeEventType.DIVIDEND;
			String key = "brapi|" + ticker + "|" + type + "|" + eligibility + "|" + payment + "|" + item.rate().stripTrailingZeros().toPlainString();
			return java.util.Optional.of(new IncomeProviderEvent(key, type, item.rate(), eligibility, payment, IncomeEventSource.BRAPI));
		} catch (RuntimeException exception) { return java.util.Optional.empty(); }
	}
	private LocalDate date(String value) { return LocalDate.parse(value.trim().substring(0, 10)); }
	private AssetProviderUnavailableException translate(RestClientResponseException exception) {
		return switch (exception.getStatusCode().value()) {
			case 401 -> authenticationFailed();
			case 403 -> new AssetProviderUnavailableException("BRAPI_PLAN_RESTRICTED", "Brapi plan does not include dividends");
			case 429 -> new AssetProviderUnavailableException("BRAPI_RATE_LIMITED", "Brapi rate limit reached");
			default -> unavailable();
		};
	}
	private AssetProviderUnavailableException authenticationFailed() { return new AssetProviderUnavailableException("BRAPI_AUTHENTICATION_FAILED", "Brapi authentication failed"); }
	private AssetProviderUnavailableException invalidResponse() { return new AssetProviderUnavailableException("BRAPI_INVALID_RESPONSE", "Brapi returned an invalid response"); }
	private AssetProviderUnavailableException unavailable() { return new AssetProviderUnavailableException("BRAPI_PROVIDER_UNAVAILABLE", "Brapi is unavailable"); }
	private boolean blank(String value) { return value == null || value.isBlank(); }
	private record BrapiIncomeResponse(List<BrapiIncomeSeries> results) { }
	private record BrapiIncomeSeries(String requestedSymbol, String symbol, Boolean changed, BrapiDividendsData data) { }
	private record BrapiDividendsData(List<BrapiCashDividend> cashDividends) { }
	private record BrapiCashDividend(BigDecimal rate, String lastDatePrior, String paymentDate, String label) { }
}
