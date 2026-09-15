package com.bominvestidor.spring.service.income;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.income.IncomeProviderEvent;
import com.bominvestidor.spring.dto.income.IncomeEventWarningResponse;
import com.bominvestidor.spring.exception.AssetProviderUnavailableException;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategy;
import com.bominvestidor.spring.integration.income.IncomeProviderBatchResult;

@Component
public class IncomeProviderEventCache {
	private final Map<Key, Entry> entries = new ConcurrentHashMap<>();
	private final Clock clock;
	private final Duration ttl;
	private final Duration staleTtl;

	public IncomeProviderEventCache(Clock clock, BrokerageIntegrationProperties properties) {
		this.clock = clock;
		this.ttl = properties.getIncomeProviderCacheTtl();
		this.staleTtl = properties.getIncomeProviderStaleTtl();
	}

	public synchronized Resolution resolve(AssetMarket market, Set<String> requestedTickers,
			IncomeEventProviderStrategy provider) {
		Instant now = clock.instant();
		Set<String> tickers = new TreeSet<>();
		requestedTickers.forEach(value -> tickers.add(value.trim().toUpperCase(Locale.ROOT)));
		Map<String, List<IncomeProviderEvent>> resolved = new LinkedHashMap<>();
		List<IncomeEventWarningResponse> warnings = new ArrayList<>();
		Set<String> refresh = new TreeSet<>();
		Map<String, Entry> previous = new LinkedHashMap<>();
		for (String ticker : tickers) {
			Entry entry = entries.get(new Key(market, ticker));
			if (entry != null && now.isBefore(entry.expiresAt())) resolved.put(ticker, entry.events());
			else { refresh.add(ticker); if (entry != null) previous.put(ticker, entry); }
		}
		if (!refresh.isEmpty()) {
			IncomeProviderBatchResult batch;
			try { batch = provider.findEvents(refresh); }
			catch (AssetProviderUnavailableException exception) {
				Map<String, AssetProviderUnavailableException> failures = new LinkedHashMap<>();
				refresh.forEach(ticker -> failures.put(ticker, exception));
				batch = new IncomeProviderBatchResult(Map.of(), failures);
			}
			for (String ticker : refresh) {
				if (batch.events().containsKey(ticker)) {
					Entry entry = new Entry(List.copyOf(batch.events().get(ticker)), now, now.plus(ttl), now.plus(ttl).plus(staleTtl));
					entries.put(new Key(market, ticker), entry);
					resolved.put(ticker, entry.events());
					continue;
				}
				AssetProviderUnavailableException failure = batch.failures().getOrDefault(ticker,
						new AssetProviderUnavailableException(providerUnavailableCode(market), "Income provider is unavailable"));
				Entry old = previous.get(ticker);
				if (old != null && now.isBefore(old.staleUntil())) resolved.put(ticker, old.events());
				warnings.add(new IncomeEventWarningResponse(ticker, market, failure.getCode()));
			}
		}
		if (!tickers.isEmpty() && resolved.isEmpty() && !warnings.isEmpty()) {
			String code = warnings.get(0).code();
			throw new AssetProviderUnavailableException(code, "No reliable income event data is available");
		}
		Instant updatedAt = tickers.isEmpty() ? now : tickers.stream().map(ticker -> entries.get(new Key(market, ticker)))
				.filter(java.util.Objects::nonNull).map(Entry::fetchedAt).min(Instant::compareTo).orElse(now);
		boolean stale = warnings.stream().anyMatch(warning -> {
			Entry old = previous.get(warning.ticker());
			return old != null && now.isBefore(old.staleUntil());
		});
		return new Resolution(resolved, updatedAt, stale, warnings);
	}

	private String providerUnavailableCode(AssetMarket market) {
		return market == AssetMarket.US ? "ALPHAVANTAGE_PROVIDER_UNAVAILABLE" : "BRAPI_PROVIDER_UNAVAILABLE";
	}

	public record Resolution(Map<String, List<IncomeProviderEvent>> events, Instant updatedAt, boolean stale,
			List<IncomeEventWarningResponse> warnings) { }
	private record Key(AssetMarket market, String ticker) { }
	private record Entry(List<IncomeProviderEvent> events, Instant fetchedAt, Instant expiresAt, Instant staleUntil) { }
}
