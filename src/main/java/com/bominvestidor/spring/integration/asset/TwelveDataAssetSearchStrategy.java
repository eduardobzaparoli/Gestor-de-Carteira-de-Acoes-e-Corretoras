package com.bominvestidor.spring.integration.asset;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetCandidate;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetQuote;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.integration.twelvedata.TwelveDataFailureTranslator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class TwelveDataAssetSearchStrategy implements AssetSearchStrategy {
	private final RestClient client;
	private final String apiKey;

	public TwelveDataAssetSearchStrategy(@Qualifier("twelveDataRestClient") RestClient client,
			BrokerageIntegrationProperties properties) {
		this.client = client;
		this.apiKey = properties.getTwelveDataApiKey();
	}

	@Override public AssetMarket market() { return AssetMarket.US; }

	@Override
	public List<AssetCandidate> findCandidates(AssetType assetType, String query) {
		try {
			SearchResponse response = client.get().uri(uri -> uri.path("/symbol_search")
					.queryParam("symbol", query).queryParam("outputsize", 30).queryParam("show_plan", true)
					.queryParam("apikey", requiredApiKey()).build()).retrieve().body(SearchResponse.class);
			if (response == null) throw TwelveDataFailureTranslator.unavailable();
			TwelveDataFailureTranslator.throwIfError(response.status(), response.code(), response.message());
			if (response.data() == null) return List.of();
			var candidatesByTicker = new LinkedHashMap<String, AssetCandidate>();
			response.data().stream()
					.filter(item -> matches(assetType, item))
					.map(item -> new AssetCandidate(item.symbol().trim().toUpperCase(Locale.ROOT),
							item.instrumentName().trim(), AssetMarket.US,
							assetType, "USD"))
					.forEach(candidate -> candidatesByTicker.putIfAbsent(candidate.ticker(), candidate));
			return List.copyOf(candidatesByTicker.values());
		} catch (RestClientResponseException exception) { throw TwelveDataFailureTranslator.translate(exception); }
		catch (RestClientException exception) { throw TwelveDataFailureTranslator.unavailable(); }
	}

	@Override
	public Optional<AssetQuote> findQuote(String ticker) {
		try {
			PriceResponse response = client.get().uri(uri -> uri.path("/price").queryParam("symbol", ticker)
					.queryParam("country", "United States").queryParam("apikey", requiredApiKey()).build())
					.retrieve().body(PriceResponse.class);
			if (response == null) throw TwelveDataFailureTranslator.unavailable();
			TwelveDataFailureTranslator.throwIfError(response.status(), response.code(), response.message());
			if (blank(response.price())) return Optional.empty();
			BigDecimal price = new BigDecimal(response.price());
			return price.signum() > 0 ? Optional.of(new AssetQuote(ticker, "USD", price)) : Optional.empty();
		} catch (NumberFormatException exception) { throw TwelveDataFailureTranslator.unavailable(); }
		catch (RestClientResponseException exception) { throw TwelveDataFailureTranslator.translate(exception); }
		catch (RestClientException exception) { throw TwelveDataFailureTranslator.unavailable(); }
	}

	private boolean matches(AssetType requestedType, SearchItem item) {
		if (item == null || blank(item.symbol()) || blank(item.instrumentName()) || blank(item.instrumentType())) return false;
		boolean us = "US".equalsIgnoreCase(item.country()) || "United States".equalsIgnoreCase(item.country());
		boolean usd = "USD".equalsIgnoreCase(item.currency());
		boolean type = requestedType == AssetType.ETF ? "ETF".equalsIgnoreCase(item.instrumentType())
				: "Common Stock".equalsIgnoreCase(item.instrumentType());
		return us && usd && type;
	}

	private String requiredApiKey() {
		if (apiKey == null || apiKey.isBlank()) throw TwelveDataFailureTranslator.unavailable();
		return apiKey;
	}

	private boolean blank(String value) { return value == null || value.isBlank(); }

	record SearchResponse(List<SearchItem> data, String status, Integer code, String message) { }
	record SearchItem(String symbol, @JsonProperty("instrument_name") String instrumentName,
			@JsonProperty("instrument_type") String instrumentType, String country, String currency) { }
	record PriceResponse(String price, String status, Integer code, String message) { }
}
