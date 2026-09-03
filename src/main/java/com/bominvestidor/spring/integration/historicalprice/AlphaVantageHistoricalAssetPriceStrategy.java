package com.bominvestidor.spring.integration.historicalprice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetKey;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPrice;
import com.bominvestidor.spring.domain.evolution.HistoricalAssetPriceSeries;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.exception.HistoricalPriceUnavailableException;

@Component
public class AlphaVantageHistoricalAssetPriceStrategy implements HistoricalAssetPriceStrategy {
	private final RestClient client;
	private final String apiKey;
	public AlphaVantageHistoricalAssetPriceStrategy(@Qualifier("alphaVantageRestClient") RestClient client, BrokerageIntegrationProperties properties) {
		this.client = client; this.apiKey = properties.getAlphaVantageApiKey();
	}
	@Override public AssetMarket market() { return AssetMarket.US; }
	@Override
	public HistoricalAssetPriceSeries findSeries(String ticker, String currency, LocalDate startDate, LocalDate endDate) {
		if (apiKey == null || apiKey.isBlank()) throw unavailable();
		try {
			AlphaResponse body = client.get().uri(uri -> uri.path("/query").queryParam("function", "TIME_SERIES_DAILY")
					.queryParam("symbol", ticker).queryParam("outputsize", "compact").queryParam("apikey", apiKey).build()).retrieve().body(AlphaResponse.class);
			if (body == null) throw unavailable();
			if (hasText(body.note()) || isRateLimit(body.information())) throw rateLimited();
			if (hasText(body.information())) throw unavailable();
			NavigableMap<LocalDate, HistoricalAssetPrice> normalized = new TreeMap<>();
			if (body.days() != null) {
				for (var field : body.days().entrySet()) {
					try {
						LocalDate date = LocalDate.parse(field.getKey());
						BigDecimal close = new BigDecimal(field.getValue().get("4. close"));
						if (!date.isBefore(startDate) && !date.isAfter(endDate) && close.signum() > 0) normalized.put(date, new HistoricalAssetPrice(date, close));
					} catch (RuntimeException ignored) { }
				}
			}
			if (normalized.isEmpty()) throw new HistoricalPriceUnavailableException();
			return new HistoricalAssetPriceSeries(new HistoricalAssetKey(market(), ticker, currency), normalized);
		} catch (RestClientResponseException exception) { throw exception.getStatusCode().value() == 429 ? rateLimited() : unavailable(); }
		catch (RestClientException exception) { throw unavailable(); }
	}
	private AssetProviderUnavailableException rateLimited() { return new AssetProviderUnavailableException("ALPHAVANTAGE_RATE_LIMITED", "Alpha Vantage rate limit reached"); }
	private AssetProviderUnavailableException unavailable() { return new AssetProviderUnavailableException("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", "Alpha Vantage historical prices are unavailable"); }
	private boolean hasText(String value) { return value != null && !value.isBlank(); }
	private boolean isRateLimit(String value) {
		if (!hasText(value)) return false;
		String normalized = value.toLowerCase(java.util.Locale.ROOT);
		return normalized.contains("rate limit") || normalized.contains("call frequency") || normalized.contains("requests per day");
	}
	private record AlphaResponse(@com.fasterxml.jackson.annotation.JsonProperty("Time Series (Daily)") java.util.Map<String, java.util.Map<String, String>> days,
			@com.fasterxml.jackson.annotation.JsonProperty("Note") String note,
			@com.fasterxml.jackson.annotation.JsonProperty("Information") String information) { }
}
