package com.bominvestidor.spring.domain.valuation;

import java.math.BigDecimal;
import java.math.MathContext;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

/** Shared historical quantity and BRL cost-basis accumulator. */
public final class HistoricalPositionAccumulator {
	private static final MathContext PRECISION = MathContext.DECIMAL128;
	private BigDecimal quantity = BigDecimal.ZERO;
	private BigDecimal costInBrl = BigDecimal.ZERO;

	public void apply(PortfolioTransactionEntity transaction, ExchangeRate purchaseRate) {
		if (transaction.getType() == TransactionType.BUY) {
			BigDecimal purchaseCost = transaction.getQuantity().multiply(transaction.getUnitPrice(), PRECISION)
					.add(transaction.getCosts(), PRECISION);
			if (!"BRL".equalsIgnoreCase(transaction.getCurrency())) {
				if (purchaseRate == null) throw new IllegalArgumentException("Missing historical exchange rate");
				purchaseCost = purchaseCost.multiply(purchaseRate.rate(), PRECISION);
			}
			quantity = quantity.add(transaction.getQuantity(), PRECISION);
			costInBrl = costInBrl.add(purchaseCost, PRECISION);
			return;
		}

		BigDecimal averageCost = quantity.signum() == 0 ? BigDecimal.ZERO : costInBrl.divide(quantity, PRECISION);
		quantity = quantity.subtract(transaction.getQuantity(), PRECISION);
		costInBrl = costInBrl.subtract(transaction.getQuantity().multiply(averageCost, PRECISION), PRECISION);
		if (quantity.signum() == 0) {
			quantity = BigDecimal.ZERO;
			costInBrl = BigDecimal.ZERO;
		}
	}

	public BigDecimal quantity() { return quantity; }
	public BigDecimal costInBrl() { return costInBrl; }
}
