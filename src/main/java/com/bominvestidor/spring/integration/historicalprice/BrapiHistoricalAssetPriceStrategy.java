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
public class BrapiHistoricalAssetPriceStrategy implements HistoricalAssetPriceStrategy {
	private final RestClient client;
	private final String token;
	public BrapiHistoricalAssetPriceStrategy(@Qualifier("brapiRestClient") RestClient client, BrokerageIntegrationProperties properties) {
		this.client = client; this.token = properties.getBrapiToken();
	}
	@Override public AssetMarket market() { return AssetMarket.BR; }
	@Override
	public HistoricalAssetPriceSeries findSeries(String ticker, String currency, LocalDate startDate, LocalDate endDate) {
		if (token == null || token.isBlank()) throw unavailable();
		try {
			BrapiResponse body = client.get().uri(uri -> uri.path("/api/quote/{ticker}").queryParam("range", "3mo")
					.queryParam("interval", "1d").build(ticker)).header("Authorization", "Bearer " + token).retrieve().body(BrapiResponse.class);
			NavigableMap<LocalDate, HistoricalAssetPrice> normalized = new TreeMap<>();
			if (body != null && body.results() != null && !body.results().isEmpty() && body.results().get(0).historicalDataPrice() != null) for (BrapiPrice item : body.results().get(0).historicalDataPrice()) {
				if (item.date() != null && item.close() != null) {
					LocalDate date = java.time.Instant.ofEpochSecond(item.date()).atZone(java.time.ZoneOffset.UTC).toLocalDate();
					BigDecimal close = item.close();
					if (!date.isBefore(startDate) && !date.isAfter(endDate) && close.signum() > 0) normalized.put(date, new HistoricalAssetPrice(date, close));
				}
			}
			if (normalized.isEmpty()) throw new HistoricalPriceUnavailableException();
			return new HistoricalAssetPriceSeries(new HistoricalAssetKey(market(), ticker, currency), normalized);
		} catch (RestClientResponseException exception) { throw translate(exception); }
		catch (RestClientException exception) { throw unavailable(); }
	}
	private RuntimeException translate(RestClientResponseException exception) {
		return exception.getStatusCode().value() == 429 ? new AssetProviderUnavailableException("BRAPI_RATE_LIMITED", "Brapi rate limit reached") : unavailable();
	}
	private AssetProviderUnavailableException unavailable() { return new AssetProviderUnavailableException("BRAPI_PROVIDER_UNAVAILABLE", "Brapi historical prices are unavailable"); }
	private record BrapiResponse(java.util.List<BrapiResult> results) { }
	private record BrapiResult(java.util.List<BrapiPrice> historicalDataPrice) { }
	private record BrapiPrice(Long date, BigDecimal close) { }
}
