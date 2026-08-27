package com.bominvestidor.spring.integration.address;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ViaCepClient implements AddressLookupStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ViaCepClient.class);

	private final RestClient restClient;

	public ViaCepClient(@Qualifier("viaCepRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	@Override
	public Optional<AddressLookupData> findByCep(String cep) {
		try {
			ViaCepResponse response = restClient.get()
					.uri("/ws/{cep}/json/", cep)
					.retrieve()
					.body(ViaCepResponse.class);
			if (response == null) {
				throw new AddressProviderUnavailableException();
			}
			if (Boolean.TRUE.equals(response.erro())) {
				return Optional.empty();
			}
			if (isBlank(response.cep()) || isBlank(response.localidade()) || isBlank(response.uf())) {
				throw new AddressProviderUnavailableException();
			}
			return Optional.of(new AddressLookupData(response.cep(), blankToNull(response.logradouro()),
					blankToNull(response.bairro()), response.localidade().trim(), response.uf().trim()));
		}
		catch (RestClientResponseException exception) {
			return unavailable(cep);
		}
		catch (RestClientException | AddressProviderUnavailableException exception) {
			return unavailable(cep);
		}
	}

	private Optional<AddressLookupData> unavailable(String cep) {
		LOGGER.warn("ViaCEP unavailable for CEP {}", mask(cep));
		throw new AddressProviderUnavailableException();
	}

	private boolean isBlank(String value) { return value == null || value.isBlank(); }

	private String blankToNull(String value) { return isBlank(value) ? null : value.trim(); }

	private String mask(String value) {
		return value == null || value.length() < 2 ? "**" : "****" + value.substring(value.length() - 2);
	}

	private record ViaCepResponse(String cep, String logradouro, String bairro, String localidade, String uf,
			Boolean erro) {
	}
}
