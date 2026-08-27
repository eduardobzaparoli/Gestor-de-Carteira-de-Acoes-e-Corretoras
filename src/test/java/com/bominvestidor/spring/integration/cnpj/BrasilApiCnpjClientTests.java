package com.bominvestidor.spring.integration.cnpj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

class BrasilApiCnpjClientTests {

	@Test
	void mapsProviderResponseToInternalModel() throws Exception {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://brasil.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://brasil.test/api/cnpj/v1/04252011000110"))
				.andRespond(withSuccess("{\"razao_social\":\"Empresa Teste\",\"nome_fantasia\":\"Teste\",\"cep\":\"04547-000\"}", MediaType.APPLICATION_JSON));

		var result = new BrasilApiCnpjClient(builder.build()).findByCnpj("04252011000110");

		assertTrue(result.isPresent());
		assertEquals("Empresa Teste", result.get().legalName());
		assertEquals("04547-000", result.get().postalCode());
		server.verify();
	}

	@Test
	void distinguishesNotFoundFromTechnicalFailure() throws Exception {
		RestClient.Builder notFoundBuilder = RestClient.builder().baseUrl("http://brasil.test");
		MockRestServiceServer notFoundServer = MockRestServiceServer.bindTo(notFoundBuilder).build();
		notFoundServer.expect(requestTo("http://brasil.test/api/cnpj/v1/04252011000110"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		assertTrue(new BrasilApiCnpjClient(notFoundBuilder.build()).findByCnpj("04252011000110").isEmpty());

		RestClient.Builder failureBuilder = RestClient.builder().baseUrl("http://brasil.test");
		MockRestServiceServer failureServer = MockRestServiceServer.bindTo(failureBuilder).build();
		failureServer.expect(requestTo("http://brasil.test/api/cnpj/v1/04252011000110"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("internal"));
		assertThrows(CnpjProviderUnavailableException.class,
				() -> new BrasilApiCnpjClient(failureBuilder.build()).findByCnpj("04252011000110"));
	}
}
