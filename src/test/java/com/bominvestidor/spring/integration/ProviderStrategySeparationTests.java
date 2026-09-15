package com.bominvestidor.spring.integration;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.integration.asset.AssetSearchStrategyResolver;
import com.bominvestidor.spring.integration.asset.TwelveDataAssetSearchStrategy;
import com.bominvestidor.spring.integration.historicalprice.HistoricalAssetPriceStrategyResolver;
import com.bominvestidor.spring.integration.historicalprice.TwelveDataHistoricalAssetPriceStrategy;
import com.bominvestidor.spring.integration.income.AlphaVantageIncomeEventProviderStrategy;
import com.bominvestidor.spring.integration.income.IncomeEventProviderStrategyResolver;

class ProviderStrategySeparationTests {

	@Test
	void routesUsMarketDataToTwelveDataAndUsDividendsToAlphaVantage() {
		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setTwelveDataApiKey("twelve-key");
		properties.setAlphaVantageApiKey("alpha-key");
		RestClient client = RestClient.builder().requestFactory((uri, method) -> {
			throw new AssertionError("Provider selection must not perform an HTTP request");
		}).build();

		var assetSearch = new TwelveDataAssetSearchStrategy(client, properties);
		var historicalPrices = new TwelveDataHistoricalAssetPriceStrategy(client, properties);
		var dividends = new AlphaVantageIncomeEventProviderStrategy(client, properties);

		assertSame(assetSearch, new AssetSearchStrategyResolver(List.of(assetSearch)).resolve(AssetMarket.US));
		assertSame(historicalPrices,
				new HistoricalAssetPriceStrategyResolver(List.of(historicalPrices)).resolve(AssetMarket.US));
		assertSame(dividends,
				new IncomeEventProviderStrategyResolver(List.of(dividends)).resolve(AssetMarket.US));
	}
}
