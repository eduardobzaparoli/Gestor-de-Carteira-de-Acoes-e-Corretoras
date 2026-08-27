package com.bominvestidor.spring.integration.cvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CvmSnapshotParser {

	static final Charset CVM_CHARSET = Charset.forName("windows-1252");
	private static final String CSV_ENTRY = "cad_intermed.csv";

	private CvmSnapshotParser() {
	}

	static Map<String, CvmParticipantData> parseZip(InputStream zipInputStream) throws IOException {
		try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(zipInputStream)) {
			java.util.zip.ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (CSV_ENTRY.equalsIgnoreCase(entry.getName())) {
					return parseCsv(new InputStreamReader(zip, CVM_CHARSET));
				}
			}
		}
		throw new IOException("Required CVM CSV entry is missing");
	}

	static Map<String, CvmParticipantData> parseCsv(Reader reader) throws IOException {
		List<List<String>> rows = parseRows(reader);
		if (rows.isEmpty()) {
			throw new IOException("CVM CSV is empty");
		}

		Map<String, Integer> columns = new HashMap<>();
		List<String> header = rows.get(0);
		for (int index = 0; index < header.size(); index++) {
			String value = header.get(index);
			if (index == 0 && value.startsWith("\uFEFF")) {
				value = value.substring(1);
			}
			columns.put(value.trim().toUpperCase(Locale.ROOT), index);
		}
		int typeIndex = requiredColumn(columns, "TP_PARTIC");
		int cnpjIndex = requiredColumn(columns, "CNPJ");
		int statusIndex = requiredColumn(columns, "SIT");

		Map<String, CvmParticipantData> participants = new LinkedHashMap<>();
		for (List<String> row : rows.subList(1, rows.size())) {
			if (row.size() <= Math.max(typeIndex, Math.max(cnpjIndex, statusIndex))) {
				continue;
			}
			String cnpj = digitsOnly(row.get(cnpjIndex));
			if (cnpj.length() != 14) {
				continue;
			}
			CvmParticipantData participant = new CvmParticipantData(cnpj, trim(row.get(typeIndex)), trim(row.get(statusIndex)));
			participants.merge(cnpj, participant, CvmSnapshotParser::preferActiveRecord);
		}
		return Map.copyOf(participants);
	}

	private static CvmParticipantData preferActiveRecord(CvmParticipantData first, CvmParticipantData second) {
		if (first.status().equalsIgnoreCase("EM FUNCIONAMENTO NORMAL")) {
			return first;
		}
		return second;
	}

	private static int requiredColumn(Map<String, Integer> columns, String name) throws IOException {
		Integer index = columns.get(name);
		if (index == null) {
			throw new IOException("Required CVM column is missing: " + name);
		}
		return index;
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private static String digitsOnly(String value) {
		return value == null ? "" : value.replaceAll("\\D", "");
	}

	private static List<List<String>> parseRows(Reader reader) throws IOException {
		List<List<String>> rows = new ArrayList<>();
		List<String> row = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		PushbackReader input = new PushbackReader(reader, 1);
		int current;
		while ((current = input.read()) != -1) {
			char character = (char) current;
			if (character == '"') {
				if (quoted) {
					int next = input.read();
					if (next == '"') {
						field.append('"');
						continue;
					}
					quoted = false;
					if (next != -1) {
						input.unread(next);
					}
					continue;
				}
				quoted = true;
			} else if (character == ';' && !quoted) {
				row.add(field.toString());
				field.setLength(0);
			} else if ((character == '\n' || character == '\r') && !quoted) {
				if (character == '\r') {
					int next = input.read();
					if (next != '\n' && next != -1) {
						input.unread(next);
					}
				}
				row.add(field.toString());
				field.setLength(0);
				if (!row.isEmpty() && !(row.size() == 1 && row.get(0).isEmpty())) {
					rows.add(List.copyOf(row));
				}
				row = new ArrayList<>();
			} else {
				field.append(character);
			}
		}
		if (quoted) {
			throw new IOException("Unclosed quoted field in CVM CSV");
		}
		if (field.length() > 0 || !row.isEmpty()) {
			row.add(field.toString());
			rows.add(List.copyOf(row));
		}
		return rows;
	}
}
