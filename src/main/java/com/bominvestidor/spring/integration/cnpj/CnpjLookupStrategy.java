package com.bominvestidor.spring.integration.cnpj;

import java.util.Optional;

public interface CnpjLookupStrategy {

	Optional<CnpjRegistrationData> findByCnpj(String cnpj);
}
