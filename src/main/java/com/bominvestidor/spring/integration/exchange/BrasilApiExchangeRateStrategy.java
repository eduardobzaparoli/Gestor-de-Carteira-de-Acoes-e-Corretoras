package com.bominvestidor.spring.integration.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.exception.ExchangeRateUnavailableException;
import com.fasterxml.jackson.annotation.JsonProperty;

@Component
public class BrasilApiExchangeRateStrategy implements ExchangeRateStrategy {
	private static final int MAX_LOOKBACK_DAYS = 14;
	private final RestClient client;

	public BrasilApiExchangeRateStrategy(@Qualifier("brasilApiRestClient") RestClient client) {
		this.client = client;
	}

	@Override
	public Optional<ExchangeRate> findClosingRate(String sourceCurrency, String targetCurrency, LocalDate requestedDate) {
		if (!"USD".equalsIgnoreCase(sourceCurrency) || !"BRL".equalsIgnoreCase(targetCurrency)) return Optional.empty();
		for (int offset = 0; offset <= MAX_LOOKBACK_DAYS; offset++) {
			LocalDate date = requestedDate.minusDays(offset);
			Optional<ExchangeRate> rate = findForDate(date);
			if (rate.isPresent()) return rate;
		}
		return Optional.empty();
	}

	private Optional<ExchangeRate> findForDate(LocalDate date) {
		try {
			BrasilApiExchangeResponse response = client.get().uri("/api/cambio/v1/cotacao/{currency}/{date}", "USD", date)
					.retrieve().body(BrasilApiExchangeResponse.class);
			if (response == null || response.quotes() == null) return Optional.empty();
			LocalDate referenceDate = response.referenceDate() == null ? date : response.referenceDate();
			return response.quotes().stream().filter(quote -> "FECHAMENTO PTAX".equalsIgnoreCase(quote.bulletinType()))
					.map(BrasilApiQuote::buyRate).filter(rate -> rate != null && rate.signum() > 0)
					.findFirst().map(rate -> new ExchangeRate("USD", "BRL", rate, referenceDate));
		} catch (RestClientResponseException exception) {
			// BrasilAPI rejects today's still-forming PTAX with 400. Treat it like a
			// missing business-day quote so the caller can use the previous close.
			if (exception.getStatusCode().value() == 400 || exception.getStatusCode().value() == 404) return Optional.empty();
			throw unavailable();
		} catch (RestClientException exception) {
			throw unavailable();
		}
	}

	private ExchangeRateUnavailableException unavailable() {
		return new ExchangeRateUnavailableException();
	}

	private record BrasilApiExchangeResponse(@JsonProperty("cotacoes") List<BrasilApiQuote> quotes,
			@JsonProperty("data") LocalDate referenceDate) { }
	private record BrasilApiQuote(@JsonProperty("cotacao_compra") BigDecimal buyRate,
			@JsonProperty("tipo_boletim") String bulletinType) { }
}
