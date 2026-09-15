package com.bominvestidor.spring.integration.cvm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;

class CvmSnapshotClientTests {

	@Test
	void downloadsSnapshotOnceWhileCacheIsValid() throws Exception {
		String url = "http://cvm.test/cad_intermed.zip";
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(url)).andRespond(withSuccess(snapshot(), MediaType.APPLICATION_OCTET_STREAM));

		BrokerageIntegrationProperties properties = new BrokerageIntegrationProperties();
		properties.setCvmSnapshotUrl(url);
		properties.setCvmCacheTtl(java.time.Duration.ofHours(1));
		CvmSnapshotClient client = new CvmSnapshotClient(builder.build(), properties,
				Clock.fixed(Instant.parse("2026-08-26T12:00:00Z"), ZoneOffset.UTC));

		var first = client.findByCnpj("04252011000110");
		var second = client.findByCnpj("99999999000191");

		assertTrue(first.isPresent());
		assertEquals("CORRETORAS", first.get().category());
		assertTrue(second.isEmpty());
		server.verify();
	}

	private byte[] snapshot() throws Exception {
		String csv = "TP_PARTIC;CNPJ;SIT\r\nCORRETORAS;04.252.011/0001-10;EM FUNCIONAMENTO NORMAL\r\n";
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(output)) {
			zip.putNextEntry(new ZipEntry("cad_intermed.csv"));
			zip.write(csv.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return output.toByteArray();
	}
}
