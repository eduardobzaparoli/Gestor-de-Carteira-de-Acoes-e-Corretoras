package com.bominvestidor.spring.service.brokerage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.dto.brokerage.BrokerageCreateRequest;
import com.bominvestidor.spring.exception.InvalidBrokerageDataException;

class BrokerageInputNormalizerTests {

	private final BrokerageInputNormalizer normalizer = new BrokerageInputNormalizer();

	@Test
	void normalizesDocumentsAndKeepsManualAddressFields() {
		NormalizedBrokerageInput input = normalizer.normalize(new BrokerageCreateRequest(
				"  Minha Corretora  ", "04.252.011/0001-10", "04547-000", "  Rua Manual  ", " Bairro ",
				" 42 ", "  Sala 1  "));

		assertEquals("Minha Corretora", input.nickname());
		assertEquals("minha corretora", input.nicknameKey());
		assertEquals("04252011000110", input.cnpj());
		assertEquals("04547000", input.cep());
		assertEquals("Rua Manual", input.street());
		assertEquals("Sala 1", input.complement());
	}

	@Test
	void rejectsInvalidCnpjWithoutAcceptingLettersAsFormatting() {
		assertThrows(InvalidBrokerageDataException.class, () -> normalizer.normalize(new BrokerageCreateRequest(
				"Corretora", "04.252.011/0001-11", "04547000", "Rua", "Bairro", "1", null)));
		assertThrows(InvalidBrokerageDataException.class, () -> normalizer.normalize(new BrokerageCreateRequest(
				"Corretora", "04A252011000110", "04547000", "Rua", "Bairro", "1", null)));
	}

	@Test
	void reportsAllRequiredFieldsAfterTrimming() {
		InvalidBrokerageDataException exception = assertThrows(InvalidBrokerageDataException.class,
				() -> normalizer.normalize(new BrokerageCreateRequest(" ", " ", " ", " ", " ", " ", null)));

		assertEquals(6, exception.getFieldErrors().size());
	}
}
