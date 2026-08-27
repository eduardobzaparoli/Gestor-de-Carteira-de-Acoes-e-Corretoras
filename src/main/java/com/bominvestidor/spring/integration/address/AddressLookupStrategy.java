package com.bominvestidor.spring.integration.address;

import java.util.Optional;

public interface AddressLookupStrategy {

	Optional<AddressLookupData> findByCep(String cep);
}
