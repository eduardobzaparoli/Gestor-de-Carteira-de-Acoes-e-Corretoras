package com.bominvestidor.spring.dto.brokerage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrokerageCreateRequest(
		@NotBlank @Size(max = 100) String nickname,
		@NotBlank String cnpj,
		@NotBlank String cep,
		@NotBlank @Size(max = 150) String street,
		@NotBlank @Size(max = 100) String neighborhood,
		@NotBlank @Size(max = 20) String number,
		@Size(max = 100) String complement) {
}
