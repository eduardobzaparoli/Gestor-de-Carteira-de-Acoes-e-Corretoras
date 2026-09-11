package com.bominvestidor.spring.integration.historicalprice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
import com.bominvestidor.spring.exception.HistoricalPriceUnavailableException;
import com.bominvestidor.spring.integration.twelvedata.TwelveDataFailureTranslator;

@Component
public class TwelveDataHistoricalAssetPriceStrategy implements HistoricalAssetPriceStrategy {
	private final RestClient client;
	private final String apiKey;

	public TwelveDataHistoricalAssetPriceStrategy(@Qualifier("twelveDataRestClient") RestClient client,
			BrokerageIntegrationProperties properties) {
		this.client = client;
		this.apiKey = properties.getTwelveDataApiKey();
	}

	@Override public AssetMarket market() { return AssetMarket.US; }

	@Override
	public HistoricalAssetPriceSeries findSeries(String ticker, String currency, LocalDate startDate, LocalDate endDate) {
		try {
			Response response = client.get().uri(uri -> uri.path("/time_series").queryParam("symbol", ticker)
					.queryParam("interval", "1day").queryParam("start_date", startDate).queryParam("end_date", endDate)
					.queryParam("adjust", "none").queryParam("apikey", requiredApiKey()).build()).retrieve().body(Response.class);
			if (response == null) throw TwelveDataFailureTranslator.unavailable();
			TwelveDataFailureTranslator.throwIfError(response.status(), response.code(), response.message());
			NavigableMap<LocalDate, HistoricalAssetPrice> normalized = new TreeMap<>();
			if (response.values() != null) response.values().forEach(item -> add(normalized, item, startDate, endDate));
			if (normalized.isEmpty()) throw new HistoricalPriceUnavailableException();
			return new HistoricalAssetPriceSeries(new HistoricalAssetKey(market(), ticker, currency), normalized);
		} catch (RestClientResponseException exception) { throw TwelveDataFailureTranslator.translate(exception); }
		catch (RestClientException exception) { throw TwelveDataFailureTranslator.unavailable(); }
	}

	private void add(NavigableMap<LocalDate, HistoricalAssetPrice> prices, Value item,
			LocalDate startDate, LocalDate endDate) {
		if (item == null) return;
		try {
			LocalDate date = LocalDate.parse(item.datetime());
			BigDecimal close = new BigDecimal(item.close());
			if (!date.isBefore(startDate) && !date.isAfter(endDate) && close.signum() > 0)
				prices.put(date, new HistoricalAssetPrice(date, close));
		} catch (RuntimeException ignored) { }
	}

	private String requiredApiKey() {
		if (apiKey == null || apiKey.isBlank()) throw TwelveDataFailureTranslator.unavailable();
		return apiKey;
	}

	private record Response(List<Value> values, String status, Integer code, String message) { }
	private record Value(String datetime, String close) { }
}
