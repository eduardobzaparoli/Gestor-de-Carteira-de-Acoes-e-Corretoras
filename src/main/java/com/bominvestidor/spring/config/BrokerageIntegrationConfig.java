package com.bominvestidor.spring.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BrokerageIntegrationProperties.class)
public class BrokerageIntegrationConfig {

	@Bean("brasilApiRestClient")
	RestClient brasilApiRestClient(BrokerageIntegrationProperties properties) {
		return client(properties.getBrasilApiBaseUrl(), properties.getConnectTimeout(), properties.getReadTimeout());
	}

	@Bean("viaCepRestClient")
	RestClient viaCepRestClient(BrokerageIntegrationProperties properties) {
		return client(properties.getViaCepBaseUrl(), properties.getConnectTimeout(), properties.getReadTimeout());
	}

	@Bean("cvmRestClient")
	RestClient cvmRestClient(BrokerageIntegrationProperties properties) {
		return RestClient.builder()
				.requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
				.build();
	}

	private RestClient client(String baseUrl, Duration connectTimeout, Duration readTimeout) {
		return RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory(connectTimeout, readTimeout))
				.build();
	}

	private SimpleClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		return factory;
	}
}
