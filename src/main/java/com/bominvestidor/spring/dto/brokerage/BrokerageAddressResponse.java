package com.bominvestidor.spring.dto.brokerage;

public record BrokerageAddressResponse(
		String cep,
		String street,
		String neighborhood,
		String number,
		String complement,
		String city,
		String state) {
}
