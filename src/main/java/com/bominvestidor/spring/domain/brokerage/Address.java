package com.bominvestidor.spring.domain.brokerage;

import java.util.Objects;

public record Address(
		String cep,
		String street,
		String neighborhood,
		String number,
		String complement,
		String city,
		String state) {

	public Address {
		Objects.requireNonNull(cep, "cep must not be null");
		Objects.requireNonNull(street, "street must not be null");
		Objects.requireNonNull(neighborhood, "neighborhood must not be null");
		Objects.requireNonNull(number, "number must not be null");
		Objects.requireNonNull(city, "city must not be null");
		Objects.requireNonNull(state, "state must not be null");
	}
}
