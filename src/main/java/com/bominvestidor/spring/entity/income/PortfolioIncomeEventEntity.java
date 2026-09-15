package com.bominvestidor.spring.entity.income;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.income.IncomeEventSource;
import com.bominvestidor.spring.domain.income.IncomeEventStatus;
import com.bominvestidor.spring.domain.income.IncomeEventType;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "portfolio_income_events", uniqueConstraints = @UniqueConstraint(
		name = "uk_portfolio_income_events_source_key", columnNames = { "portfolio_id", "source", "event_key" }))
public class PortfolioIncomeEventEntity {
	@Id @Column(nullable = false, updatable = false) private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "portfolio_id", nullable = false, updatable = false)
	private PortfolioEntity portfolio;
	@Column(nullable = false, length = 32, updatable = false) private String ticker;
	@Column(nullable = false, length = 200, updatable = false) private String assetName;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 8, updatable = false) private AssetMarket market;
	@Enumerated(EnumType.STRING) @Column(name = "asset_type", nullable = false, length = 8, updatable = false) private AssetType assetType;
	@Column(nullable = false, length = 8, updatable = false) private String currency;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 24, updatable = false) private IncomeEventType type;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, updatable = false) private IncomeEventSource source;
	@Column(name = "event_key", nullable = false, length = 512, updatable = false) private String eventKey;
	@Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private IncomeEventStatus status;
	@Column(name = "eligibility_date", updatable = false) private LocalDate eligibilityDate;
	@Column(name = "payment_date", nullable = false, updatable = false) private LocalDate paymentDate;
	@Column(name = "eligible_quantity", precision = 19, scale = 8, updatable = false) private BigDecimal eligibleQuantity;
	@Column(name = "unit_amount", precision = 19, scale = 8, updatable = false) private BigDecimal unitAmount;
	@Column(name = "expected_amount", precision = 19, scale = 8, updatable = false) private BigDecimal expectedAmount;
	@Column(name = "received_amount", nullable = false, precision = 19, scale = 8, updatable = false) private BigDecimal receivedAmount;
	@Column(name = "adjustment_reason", length = 500, updatable = false) private String adjustmentReason;
	@Column(name = "notes", length = 500, updatable = false) private String notes;
	@Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
	protected PortfolioIncomeEventEntity() { }
	public PortfolioIncomeEventEntity(UUID id, PortfolioEntity portfolio, String ticker, String assetName, AssetMarket market,
			AssetType assetType, String currency, IncomeEventType type, IncomeEventSource source, String eventKey,
			IncomeEventStatus status, LocalDate eligibilityDate, LocalDate paymentDate, BigDecimal eligibleQuantity,
			BigDecimal unitAmount, BigDecimal expectedAmount, BigDecimal receivedAmount, String adjustmentReason,
			String notes, Instant createdAt, Instant updatedAt) {
		this.id=id; this.portfolio=portfolio; this.ticker=ticker; this.assetName=assetName; this.market=market;
		this.assetType=assetType; this.currency=currency; this.type=type; this.source=source; this.eventKey=eventKey;
		this.status=status; this.eligibilityDate=eligibilityDate; this.paymentDate=paymentDate; this.eligibleQuantity=eligibleQuantity;
		this.unitAmount=unitAmount; this.expectedAmount=expectedAmount; this.receivedAmount=receivedAmount;
		this.adjustmentReason=adjustmentReason; this.notes=notes; this.createdAt=createdAt; this.updatedAt=updatedAt;
	}
	public void effective(Instant now) { status = IncomeEventStatus.EFFECTIVE; updatedAt = now; }
	public void cancel(Instant now) { status = IncomeEventStatus.CANCELLED; updatedAt = now; }
	public UUID getId(){return id;} public PortfolioEntity getPortfolio(){return portfolio;} public String getTicker(){return ticker;}
	public String getAssetName(){return assetName;} public AssetMarket getMarket(){return market;} public AssetType getAssetType(){return assetType;}
	public String getCurrency(){return currency;} public IncomeEventType getType(){return type;} public IncomeEventSource getSource(){return source;}
	public String getEventKey(){return eventKey;} public IncomeEventStatus getStatus(){return status;} public LocalDate getEligibilityDate(){return eligibilityDate;}
	public LocalDate getPaymentDate(){return paymentDate;} public BigDecimal getEligibleQuantity(){return eligibleQuantity;} public BigDecimal getUnitAmount(){return unitAmount;}
	public BigDecimal getExpectedAmount(){return expectedAmount;} public BigDecimal getReceivedAmount(){return receivedAmount;}
	public String getAdjustmentReason(){return adjustmentReason;} public String getNotes(){return notes;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
