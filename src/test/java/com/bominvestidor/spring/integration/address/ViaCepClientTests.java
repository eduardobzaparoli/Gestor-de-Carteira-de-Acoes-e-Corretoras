package com.bominvestidor.spring.integration.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

class ViaCepClientTests {

	@Test
	void keepsMissingStreetAndNeighborhoodAsOptionalInternalValues() throws Exception {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://viacep.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://viacep.test/ws/04547000/json/"))
				.andRespond(withSuccess("{\"cep\":\"04547-000\",\"logradouro\":\"\",\"bairro\":\"\",\"localidade\":\"São Paulo\",\"uf\":\"SP\"}", MediaType.APPLICATION_JSON));

		var result = new ViaCepClient(builder.build()).findByCep("04547000");

		assertTrue(result.isPresent());
		org.junit.jupiter.api.Assertions.assertNull(result.get().street());
		assertEquals("São Paulo", result.get().city());
		server.verify();
	}

	@Test
	void representsViaCepNotFoundAsEmptyResult() throws Exception {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://viacep.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://viacep.test/ws/04547000/json/"))
				.andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));
		assertTrue(new ViaCepClient(builder.build()).findByCep("04547000").isEmpty());
		server.verify();
	}

	@Test
	void translatesUnexpectedResponseIntoProviderFailure() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://viacep.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo("http://viacep.test/ws/04547000/json/"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		org.junit.jupiter.api.Assertions.assertThrows(AddressProviderUnavailableException.class,
				() -> new ViaCepClient(builder.build()).findByCep("04547000"));
		server.verify();
	}
}
