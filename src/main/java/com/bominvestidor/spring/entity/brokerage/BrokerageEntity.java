package com.bominvestidor.spring.entity.brokerage;

import java.time.Instant;
import java.util.UUID;

import com.bominvestidor.spring.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "brokerages", uniqueConstraints = {
		@UniqueConstraint(name = "uk_brokerages_owner_cnpj", columnNames = { "owner_id", "cnpj" }),
		@UniqueConstraint(name = "uk_brokerages_owner_nickname_key", columnNames = { "owner_id", "nickname_key" })
})
public class BrokerageEntity {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false)
	private UserEntity owner;

	@Column(nullable = false, length = 100)
	private String nickname;

	@Column(name = "nickname_key", nullable = false, length = 100)
	private String nicknameKey;

	@Column(nullable = false, length = 14)
	private String cnpj;

	@Column(name = "legal_name", nullable = false, length = 255)
	private String legalName;

	@Column(name = "trade_name", length = 255)
	private String tradeName;

	@Column(name = "registration_status", nullable = false, length = 100)
	private String registrationStatus;

	@Column(name = "cvm_participant_category", nullable = false, length = 150)
	private String cvmParticipantCategory;

	@Column(nullable = false, length = 8)
	private String cep;

	@Column(nullable = false, length = 150)
	private String street;

	@Column(nullable = false, length = 100)
	private String neighborhood;

	@Column(nullable = false, length = 20)
	private String number;

	@Column(length = 100)
	private String complement;

	@Column(nullable = false, length = 150)
	private String city;

	@Column(nullable = false, length = 2)
	private String state;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected BrokerageEntity() {
	}

	public BrokerageEntity(UUID id, UserEntity owner, String nickname, String nicknameKey, String cnpj,
			String legalName, String tradeName, String registrationStatus, String cvmParticipantCategory,
			String cep, String street, String neighborhood, String number, String complement, String city,
			String state, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.owner = owner;
		this.nickname = nickname;
		this.nicknameKey = nicknameKey;
		this.cnpj = cnpj;
		this.legalName = legalName;
		this.tradeName = tradeName;
		this.registrationStatus = registrationStatus;
		this.cvmParticipantCategory = cvmParticipantCategory;
		this.cep = cep;
		this.street = street;
		this.neighborhood = neighborhood;
		this.number = number;
		this.complement = complement;
		this.city = city;
		this.state = state;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = now;
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() { return id; }
	public UserEntity getOwner() { return owner; }
	public String getNickname() { return nickname; }
	public String getNicknameKey() { return nicknameKey; }
	public String getCnpj() { return cnpj; }
	public String getLegalName() { return legalName; }
	public String getTradeName() { return tradeName; }
	public String getRegistrationStatus() { return registrationStatus; }
	public String getCvmParticipantCategory() { return cvmParticipantCategory; }
	public String getCep() { return cep; }
	public String getStreet() { return street; }
	public String getNeighborhood() { return neighborhood; }
	public String getNumber() { return number; }
	public String getComplement() { return complement; }
	public String getCity() { return city; }
	public String getState() { return state; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
