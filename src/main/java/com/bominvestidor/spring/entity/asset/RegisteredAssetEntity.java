package com.bominvestidor.spring.entity.asset;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.entity.user.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "registered_assets", uniqueConstraints = @UniqueConstraint(
		name = "uk_registered_assets_owner_market_ticker", columnNames = { "owner_id", "market", "ticker" }))
public class RegisteredAssetEntity {
	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false, updatable = false)
	private UserEntity owner;

	@Column(nullable = false, length = 32)
	private String ticker;

	@Column(name = "asset_name", nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 8)
	private AssetMarket market;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type", nullable = false, length = 8)
	private AssetType assetType;

	@Column(nullable = false, length = 8)
	private String currency;

	@Column(name = "last_quote", nullable = false, precision = 19, scale = 8)
	private BigDecimal lastQuote;

	@Column(name = "quoted_at", nullable = false)
	private Instant quotedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RegisteredAssetEntity() {
	}

	public RegisteredAssetEntity(UUID id, UserEntity owner, String ticker, String name, AssetMarket market,
			AssetType assetType, String currency, BigDecimal lastQuote, Instant quotedAt, Instant createdAt,
			Instant updatedAt) {
		this.id = id;
		this.owner = owner;
		this.ticker = ticker;
		this.name = name;
		this.market = market;
		this.assetType = assetType;
		this.currency = currency;
		this.lastQuote = lastQuote;
		this.quotedAt = quotedAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public void updateQuote(BigDecimal quote, Instant quotedAt) {
		this.lastQuote = quote;
		this.quotedAt = quotedAt;
		this.updatedAt = quotedAt;
	}

	public UUID getId() { return id; }
	public UserEntity getOwner() { return owner; }
	public String getTicker() { return ticker; }
	public String getName() { return name; }
	public AssetMarket getMarket() { return market; }
	public AssetType getAssetType() { return assetType; }
	public String getCurrency() { return currency; }
	public BigDecimal getLastQuote() { return lastQuote; }
	public Instant getQuotedAt() { return quotedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
