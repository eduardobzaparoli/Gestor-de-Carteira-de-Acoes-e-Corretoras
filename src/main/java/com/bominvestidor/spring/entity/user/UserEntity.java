package com.bominvestidor.spring.entity.user;

import java.time.Instant;
import java.util.UUID;

import com.bominvestidor.spring.domain.user.UserRole;
import com.bominvestidor.spring.domain.user.UserStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class UserEntity {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserEntity() {
	}

	public UserEntity(UUID id, String name, String email, String passwordHash, UserRole role, UserStatus status,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UserEntity(UUID id, String name, String email, String passwordHash, UserRole role,
			Instant createdAt, Instant updatedAt) {
		this(id, name, email, passwordHash, role, UserStatus.ACTIVE, createdAt, updatedAt);
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
		if (status == null) {
			status = UserStatus.ACTIVE;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public UserRole getRole() {
		return role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void update(String name, String email, String passwordHash, UserRole role) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public void updateProfile(String name, String email, String passwordHash) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	public void changeStatus(UserStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
