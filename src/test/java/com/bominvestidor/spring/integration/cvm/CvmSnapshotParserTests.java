package com.bominvestidor.spring.integration.cvm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

class CvmSnapshotParserTests {

	private static final Charset CVM_CHARSET = Charset.forName("windows-1252");

	@Test
	void readsOfficialColumnsAndQuotedFieldsFromZip() throws Exception {
		String csv = "TP_PARTIC;CNPJ;DENOM_SOCIAL;DENOM_COMERC;SIT\r\n"
				+ "\"CORRETORAS; ESPECIAIS\";04.252.011/0001-10;\"Empresa; Exemplo\";Exemplo;EM FUNCIONAMENTO NORMAL\r\n"
				+ "BANCOS;11.222.333/0001-81;Banco;Banco;CANCELADA\r\n";

		Map<String, CvmParticipantData> participants = CvmSnapshotParser
				.parseZip(new ByteArrayInputStream(zip(csv)));

		assertEquals("CORRETORAS; ESPECIAIS", participants.get("04252011000110").category());
		assertEquals("EM FUNCIONAMENTO NORMAL", participants.get("04252011000110").status());
		assertEquals("CANCELADA", participants.get("11222333000181").status());
	}

	@Test
	void failsClosedWhenRequiredColumnIsMissing() {
		assertThrows(IOException.class, () -> CvmSnapshotParser.parseCsv(
				new java.io.StringReader("CNPJ;SIT\n04252011000110;EM FUNCIONAMENTO NORMAL\n")));
	}

	private byte[] zip(String csv) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry("cad_intermed.csv"));
			zip.write(csv.getBytes(CVM_CHARSET));
			zip.closeEntry();
		}
		return bytes.toByteArray();
	}
}
