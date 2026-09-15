package com.bominvestidor.spring.mapper.brokerage;

import org.springframework.stereotype.Component;

import com.bominvestidor.spring.domain.brokerage.Address;
import com.bominvestidor.spring.domain.brokerage.Brokerage;
import com.bominvestidor.spring.dto.brokerage.BrokerageAddressResponse;
import com.bominvestidor.spring.dto.brokerage.BrokerageResponse;
import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
import com.bominvestidor.spring.entity.user.UserEntity;

@Component
public class BrokerageMapper {

	public Brokerage toDomain(BrokerageEntity entity) {
		return new Brokerage(entity.getId(), entity.getOwner().getId(), entity.getNickname(), entity.getNicknameKey(),
				entity.getCnpj(), entity.getLegalName(), entity.getTradeName(), entity.getRegistrationStatus(),
				entity.getCvmParticipantCategory(), new Address(entity.getCep(), entity.getStreet(), entity.getNeighborhood(),
						entity.getNumber(), entity.getComplement(), entity.getCity(), entity.getState()),
				entity.getCreatedAt(), entity.getUpdatedAt());
	}

	public BrokerageEntity toEntity(Brokerage brokerage, UserEntity owner) {
		Address address = brokerage.address();
		return new BrokerageEntity(brokerage.id(), owner, brokerage.nickname(), brokerage.nicknameKey(), brokerage.cnpj(),
				brokerage.legalName(), brokerage.tradeName(), brokerage.registrationStatus(), brokerage.cvmParticipantCategory(),
				address.cep(), address.street(), address.neighborhood(), address.number(), address.complement(), address.city(),
				address.state(), brokerage.createdAt(), brokerage.updatedAt());
	}

	public BrokerageResponse toResponse(Brokerage brokerage) {
		Address address = brokerage.address();
		return new BrokerageResponse(brokerage.id(), brokerage.nickname(), brokerage.cnpj(), brokerage.legalName(),
				brokerage.tradeName(), brokerage.registrationStatus(), brokerage.cvmParticipantCategory(),
				new BrokerageAddressResponse(address.cep(), address.street(), address.neighborhood(), address.number(),
						address.complement(), address.city(), address.state()), brokerage.createdAt(), brokerage.updatedAt());
	}
}
