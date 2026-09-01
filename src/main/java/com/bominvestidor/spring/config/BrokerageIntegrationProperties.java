package com.bominvestidor.spring.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.integrations")
public class BrokerageIntegrationProperties {

	private String brasilApiBaseUrl = "https://brasilapi.com.br";
	private String viaCepBaseUrl = "https://viacep.com.br";
	private String cvmSnapshotUrl = "https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip";
	private Duration connectTimeout = Duration.ofSeconds(3);
	private Duration readTimeout = Duration.ofSeconds(5);
	private Duration cvmCacheTtl = Duration.ofHours(24);
	private String cvmActiveStatus = "EM FUNCIONAMENTO NORMAL";
	private long cvmMaxSnapshotBytes = 10_000_000L;
	private String brapiBaseUrl = "https://brapi.dev";
	private String brapiToken = "";
	private String alphaVantageBaseUrl = "https://www.alphavantage.co";
	private String alphaVantageApiKey = "";
	private Duration assetSearchCacheTtl = Duration.ofMinutes(5);
	private Duration assetQuoteCacheTtl = Duration.ofMinutes(1);
	private Duration exchangeRateCacheTtl = Duration.ofHours(1);
	private Duration incomeCandidateCacheTtl = Duration.ofMinutes(10);

	public String getBrasilApiBaseUrl() {
		return brasilApiBaseUrl;
	}

	public void setBrasilApiBaseUrl(String brasilApiBaseUrl) {
		this.brasilApiBaseUrl = brasilApiBaseUrl;
	}

	public String getViaCepBaseUrl() {
		return viaCepBaseUrl;
	}

	public void setViaCepBaseUrl(String viaCepBaseUrl) {
		this.viaCepBaseUrl = viaCepBaseUrl;
	}

	public String getCvmSnapshotUrl() {
		return cvmSnapshotUrl;
	}

	public void setCvmSnapshotUrl(String cvmSnapshotUrl) {
		this.cvmSnapshotUrl = cvmSnapshotUrl;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public Duration getCvmCacheTtl() {
		return cvmCacheTtl;
	}

	public void setCvmCacheTtl(Duration cvmCacheTtl) {
		this.cvmCacheTtl = cvmCacheTtl;
	}

	public String getCvmActiveStatus() {
		return cvmActiveStatus;
	}

	public void setCvmActiveStatus(String cvmActiveStatus) {
		this.cvmActiveStatus = cvmActiveStatus;
	}

	public long getCvmMaxSnapshotBytes() {
		return cvmMaxSnapshotBytes;
	}

	public void setCvmMaxSnapshotBytes(long cvmMaxSnapshotBytes) {
		this.cvmMaxSnapshotBytes = cvmMaxSnapshotBytes;
	}

	public String getBrapiBaseUrl() { return brapiBaseUrl; }
	public void setBrapiBaseUrl(String brapiBaseUrl) { this.brapiBaseUrl = brapiBaseUrl; }
	public String getBrapiToken() { return brapiToken; }
	public void setBrapiToken(String brapiToken) { this.brapiToken = brapiToken; }
	public String getAlphaVantageBaseUrl() { return alphaVantageBaseUrl; }
	public void setAlphaVantageBaseUrl(String alphaVantageBaseUrl) { this.alphaVantageBaseUrl = alphaVantageBaseUrl; }
	public String getAlphaVantageApiKey() { return alphaVantageApiKey; }
	public void setAlphaVantageApiKey(String alphaVantageApiKey) { this.alphaVantageApiKey = alphaVantageApiKey; }
	public Duration getAssetSearchCacheTtl() { return assetSearchCacheTtl; }
	public void setAssetSearchCacheTtl(Duration assetSearchCacheTtl) { this.assetSearchCacheTtl = assetSearchCacheTtl; }
	public Duration getAssetQuoteCacheTtl() { return assetQuoteCacheTtl; }
	public void setAssetQuoteCacheTtl(Duration assetQuoteCacheTtl) { this.assetQuoteCacheTtl = assetQuoteCacheTtl; }
	public Duration getExchangeRateCacheTtl() { return exchangeRateCacheTtl; }
	public void setExchangeRateCacheTtl(Duration exchangeRateCacheTtl) { this.exchangeRateCacheTtl = exchangeRateCacheTtl; }
	public Duration getIncomeCandidateCacheTtl() { return incomeCandidateCacheTtl; }
	public void setIncomeCandidateCacheTtl(Duration incomeCandidateCacheTtl) { this.incomeCandidateCacheTtl = incomeCandidateCacheTtl; }
}
