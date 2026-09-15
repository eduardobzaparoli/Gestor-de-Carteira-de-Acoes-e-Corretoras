package com.bominvestidor.spring.integration.cnpj;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BrasilApiCnpjClient implements CnpjLookupStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(BrasilApiCnpjClient.class);

	private final RestClient restClient;

	public BrasilApiCnpjClient(@Qualifier("brasilApiRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public Optional<CnpjRegistrationData> findByCnpj(String cnpj) {
		try {
			BrasilApiResponse response = restClient.get()
					.uri("/api/cnpj/v1/{cnpj}", cnpj)
					.retrieve()
					.body(BrasilApiResponse.class);
			if (response == null || isBlank(response.razao_social()) || isBlank(response.cep())) {
				throw new CnpjProviderUnavailableException();
			}
			return Optional.of(new CnpjRegistrationData(cnpj, response.razao_social().trim(), blankToNull(response.nome_fantasia()),
					response.cep()));
		}
		catch (RestClientResponseException exception) {
			if (exception.getStatusCode().value() == 404) {
				return Optional.empty();
			}
			return unavailable(cnpj);
		}
		catch (RestClientException | CnpjProviderUnavailableException exception) {
			return unavailable(cnpj);
		}
	}

	private Optional<CnpjRegistrationData> unavailable(String cnpj) {
		LOGGER.warn("Brasil API unavailable for CNPJ {}", mask(cnpj));
		throw new CnpjProviderUnavailableException();
	}

	private boolean isBlank(String value) { return value == null || value.isBlank(); }

	private String blankToNull(String value) { return isBlank(value) ? null : value.trim(); }

	private String mask(String value) {
		return value == null || value.length() < 4 ? "****" : "****" + value.substring(value.length() - 4);
	}

	private record BrasilApiResponse(String razao_social, String nome_fantasia, String cep) {
	}
}
