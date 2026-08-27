package com.bominvestidor.spring.integration.cvm;

import java.util.Optional;

public interface CvmParticipantStrategy {

	Optional<CvmParticipantData> findByCnpj(String cnpj);
}
