package com.bominvestidor.spring.dto.brokerage;

public record CepLookupResponse(String cep, String street, String neighborhood, String city, String state) {
}
