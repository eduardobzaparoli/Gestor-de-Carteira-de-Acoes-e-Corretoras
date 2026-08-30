package com.bominvestidor.spring.entity.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.portfolio.PortfolioEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio_transactions")
public class PortfolioTransactionEntity {
	@Id @Column(nullable = false, updatable = false) private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "portfolio_id", nullable = false, updatable = false)
	private PortfolioEntity portfolio;
	@Column(nullable = false, length = 32, updatable = false) private String ticker;
	@Column(nullable = false, length = 200, updatable = false) private String assetName;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 8, updatable = false) private AssetMarket market;
	@Enumerated(EnumType.STRING) @Column(name = "asset_type", nullable = false, length = 8, updatable = false) private AssetType assetType;
	@Column(nullable = false, length = 8, updatable = false) private String currency;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 8, updatable = false) private TransactionType type;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private TransactionStatus status;
	@Column(name = "transaction_date", nullable = false, updatable = false) private LocalDate transactionDate;
	@Column(nullable = false, precision = 19, scale = 8, updatable = false) private BigDecimal quantity;
	@Column(name = "unit_price", nullable = false, precision = 19, scale = 8, updatable = false) private BigDecimal unitPrice;
	@Column(nullable = false, precision = 19, scale = 8, updatable = false) private BigDecimal costs;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
	protected PortfolioTransactionEntity() { }
	public PortfolioTransactionEntity(UUID id, PortfolioEntity portfolio, String ticker, String assetName, AssetMarket market,
			AssetType assetType, String currency, TransactionType type, TransactionStatus status, LocalDate transactionDate,
			BigDecimal quantity, BigDecimal unitPrice, BigDecimal costs, Instant createdAt, Instant updatedAt) {
		this.id=id; this.portfolio=portfolio; this.ticker=ticker; this.assetName=assetName; this.market=market; this.assetType=assetType;
		this.currency=currency; this.type=type; this.status=status; this.transactionDate=transactionDate; this.quantity=quantity;
		this.unitPrice=unitPrice; this.costs=costs; this.createdAt=createdAt; this.updatedAt=updatedAt;
	}
	public void effective(Instant now) { status = TransactionStatus.EFFECTIVE; updatedAt = now; }
	public void cancel(Instant now) { status = TransactionStatus.CANCELLED; updatedAt = now; }
	public UUID getId(){return id;} public PortfolioEntity getPortfolio(){return portfolio;} public String getTicker(){return ticker;}
	public String getAssetName(){return assetName;} public AssetMarket getMarket(){return market;} public AssetType getAssetType(){return assetType;}
	public String getCurrency(){return currency;} public TransactionType getType(){return type;} public TransactionStatus getStatus(){return status;}
	public LocalDate getTransactionDate(){return transactionDate;} public BigDecimal getQuantity(){return quantity;} public BigDecimal getUnitPrice(){return unitPrice;}
	public BigDecimal getCosts(){return costs;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
