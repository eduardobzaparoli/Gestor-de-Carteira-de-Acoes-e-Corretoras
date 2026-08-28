package com.bominvestidor.spring.entity.portfolio;

import java.time.Instant;
import java.util.UUID;

import com.bominvestidor.spring.entity.brokerage.BrokerageEntity;
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
@Table(name = "portfolios", uniqueConstraints =
		@UniqueConstraint(name = "uk_portfolios_owner_name_key", columnNames = { "owner_id", "name_key" }))
public class PortfolioEntity {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false)
	private UserEntity owner;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "brokerage_id", nullable = false, updatable = false)
	private BrokerageEntity brokerage;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "name_key", nullable = false, length = 100)
	private String nameKey;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PortfolioEntity() {
	}

	public PortfolioEntity(UUID id, UserEntity owner, BrokerageEntity brokerage, String name, String nameKey,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.owner = owner;
		this.brokerage = brokerage;
		this.name = name;
		this.nameKey = nameKey;
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
	public BrokerageEntity getBrokerage() { return brokerage; }
	public String getName() { return name; }
	public String getNameKey() { return nameKey; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
