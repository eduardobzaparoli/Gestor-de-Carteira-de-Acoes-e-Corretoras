package com.bominvestidor.spring.integration.asset;

import java.math.BigDecimal;
import java.util.List;
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
public class BrapiAssetSearchStrategy implements AssetSearchStrategy {
	private final RestClient client;
	private final String token;

	public BrapiAssetSearchStrategy(@Qualifier("brapiRestClient") RestClient client,
			BrokerageIntegrationProperties properties) {
		this.client = client;
		this.token = properties.getBrapiToken();
	}

	@Override public AssetMarket market() { return AssetMarket.BR; }

	@Override
	public List<AssetCandidate> findCandidates(AssetType assetType, String query) {
		try {
			BrapiListResponse response = request("/api/quote/list?search={query}&limit=20", query)
					.retrieve().body(BrapiListResponse.class);
			if (response == null || response.stocks() == null) throw unavailable();
			return response.stocks().stream()
					.filter(item -> matches(assetType, item))
					.map(item -> new AssetCandidate(item.stock().trim(), name(item), AssetMarket.BR, assetType, "BRL"))
					.toList();
		} catch (RestClientResponseException exception) { throw translate(exception); }
		catch (RestClientException exception) { throw unavailable(); }
	}

	@Override
	public Optional<AssetQuote> findQuote(String ticker) {
		try {
			BrapiQuoteResponse response = request("/api/quote/{ticker}", ticker).retrieve().body(BrapiQuoteResponse.class);
			if (response == null || response.results() == null || response.results().isEmpty()) return Optional.empty();
			BrapiQuote quote = response.results().get(0);
			if (quote.regularMarketPrice() == null) return Optional.empty();
			return Optional.of(new AssetQuote(ticker, blankTo(quote.currency(), "BRL"), quote.regularMarketPrice()));
		} catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 404) return Optional.empty();
			throw translate(exception);
		} catch (RestClientException exception) { throw unavailable(); }
	}

	private RestClient.RequestHeadersSpec<?> request(String uri, Object... variables) {
		if (token == null || token.isBlank()) throw unavailable();
		return client.get().uri(uri, variables).header("Authorization", "Bearer " + token);
	}
	private boolean matches(AssetType type, BrapiStock item) {
		if (item == null || blank(item.stock())) return false;
		return type == AssetType.STOCK ? "stock".equalsIgnoreCase(item.subType())
				: "etf".equalsIgnoreCase(item.subType());
	}
	private String name(BrapiStock item) { return blankTo(item.name(), item.stock().trim()); }
	private AssetProviderUnavailableException translate(RestClientResponseException exception) {
		return exception.getStatusCode().value() == 429
				? new AssetProviderUnavailableException("BRAPI_RATE_LIMITED", "Brapi rate limit reached") : unavailable();
	}
	private AssetProviderUnavailableException unavailable() {
		return new AssetProviderUnavailableException("BRAPI_PROVIDER_UNAVAILABLE", "Brapi is unavailable");
	}
	private boolean blank(String value) { return value == null || value.isBlank(); }
	private String blankTo(String value, String fallback) { return blank(value) ? fallback : value.trim(); }
	private record BrapiListResponse(List<BrapiStock> stocks) { }
	private record BrapiStock(String stock, String name, String type, String subType) { }
	private record BrapiQuoteResponse(List<BrapiQuote> results) { }
	private record BrapiQuote(String symbol, String currency, BigDecimal regularMarketPrice) { }
}
