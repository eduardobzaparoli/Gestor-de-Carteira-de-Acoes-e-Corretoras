package com.bominvestidor.spring.integration.asset;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;

@Component
public class AlphaVantageAssetSearchStrategy implements AssetSearchStrategy {
	private final RestClient client;
	private final String apiKey;

	public AlphaVantageAssetSearchStrategy(@Qualifier("alphaVantageRestClient") RestClient client,
			BrokerageIntegrationProperties properties) {
		this.client = client;
		this.apiKey = properties.getAlphaVantageApiKey();
	}
	@Override public AssetMarket market() { return AssetMarket.US; }

	@Override
	public List<AssetCandidate> findCandidates(AssetType assetType, String query) {
		try {
			AlphaSearchResponse response = client.get().uri(uri -> uri.path("/query")
					.queryParam("function", "SYMBOL_SEARCH").queryParam("keywords", query)
					.queryParam("apikey", requiredApiKey()).build()).retrieve().body(AlphaSearchResponse.class);
			if (response == null) throw unavailable();
			if (response.note() != null || response.information() != null) throw rateLimited();
			return response.bestMatches() == null ? List.of() : response.bestMatches().stream()
					.filter(match -> matches(assetType, match))
					.map(match -> new AssetCandidate(match.symbol().trim(), match.name().trim(), AssetMarket.US, assetType,
							blankTo(match.currency(), "USD"))).toList();
		} catch (RestClientResponseException exception) { throw translate(exception); }
		catch (RestClientException exception) { throw unavailable(); }
	}

	@Override
	public Optional<AssetQuote> findQuote(String ticker) {
		try {
			AlphaQuoteResponse response = client.get().uri(uri -> uri.path("/query")
					.queryParam("function", "GLOBAL_QUOTE").queryParam("symbol", ticker)
					.queryParam("apikey", requiredApiKey()).build()).retrieve().body(AlphaQuoteResponse.class);
			if (response == null) throw unavailable();
			if (response.note() != null || response.information() != null) throw rateLimited();
			String price = response.globalQuote() == null ? null : response.globalQuote().get("05. price");
			return blank(price) ? Optional.empty() : Optional.of(new AssetQuote(ticker, "USD", new BigDecimal(price)));
		} catch (NumberFormatException exception) { return Optional.empty(); }
		catch (RestClientResponseException exception) { throw translate(exception); }
		catch (RestClientException exception) { throw unavailable(); }
	}

	private boolean matches(AssetType type, AlphaMatch match) {
		if (match == null || blank(match.symbol()) || blank(match.name()) || !"United States".equalsIgnoreCase(match.region())) return false;
		return type == AssetType.ETF ? "ETF".equalsIgnoreCase(match.type()) : "EQUITY".equalsIgnoreCase(match.type());
	}
	private String requiredApiKey() { if (apiKey == null || apiKey.isBlank()) throw unavailable(); return apiKey; }
	private AssetProviderUnavailableException translate(RestClientResponseException exception) {
		return exception.getStatusCode().value() == 429 ? rateLimited() : unavailable();
	}
	private AssetProviderUnavailableException rateLimited() {
		return new AssetProviderUnavailableException("ALPHAVANTAGE_RATE_LIMITED", "Alpha Vantage rate limit reached");
	}
	private AssetProviderUnavailableException unavailable() {
		return new AssetProviderUnavailableException("ALPHAVANTAGE_PROVIDER_UNAVAILABLE", "Alpha Vantage is unavailable");
	}
	private boolean blank(String value) { return value == null || value.isBlank(); }
	private String blankTo(String value, String fallback) { return blank(value) ? fallback : value.trim(); }
	record AlphaSearchResponse(@com.fasterxml.jackson.annotation.JsonProperty("bestMatches") List<AlphaMatch> bestMatches,
			@com.fasterxml.jackson.annotation.JsonProperty("Note") String note,
			@com.fasterxml.jackson.annotation.JsonProperty("Information") String information) { }
	private record AlphaMatch(String symbol, String name, String type, String region, String currency) {
		private AlphaMatch(@com.fasterxml.jackson.annotation.JsonProperty("1. symbol") String symbol,
				@com.fasterxml.jackson.annotation.JsonProperty("2. name") String name,
				@com.fasterxml.jackson.annotation.JsonProperty("3. type") String type,
				@com.fasterxml.jackson.annotation.JsonProperty("4. region") String region,
				@com.fasterxml.jackson.annotation.JsonProperty("8. currency") String currency) { this.symbol = symbol; this.name = name; this.type = type; this.region = region; this.currency = currency; }
	}
	record AlphaQuoteResponse(@com.fasterxml.jackson.annotation.JsonProperty("Global Quote") Map<String, String> globalQuote,
			@com.fasterxml.jackson.annotation.JsonProperty("Note") String note,
			@com.fasterxml.jackson.annotation.JsonProperty("Information") String information) { }
}
