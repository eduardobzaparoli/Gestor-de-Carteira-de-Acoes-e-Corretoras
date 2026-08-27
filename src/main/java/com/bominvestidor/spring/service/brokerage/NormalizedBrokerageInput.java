package com.bominvestidor.spring.service.brokerage;

public record NormalizedBrokerageInput(
		String nickname,
		String nicknameKey,
		String cnpj,
		String cep,
		String street,
		String neighborhood,
		String number,
		String complement) {
}
