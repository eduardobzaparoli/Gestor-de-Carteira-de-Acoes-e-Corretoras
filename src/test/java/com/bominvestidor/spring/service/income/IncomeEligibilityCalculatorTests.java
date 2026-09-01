package com.bominvestidor.spring.service.income;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.asset.AssetMarket;
import com.bominvestidor.spring.domain.asset.AssetType;
import com.bominvestidor.spring.domain.transaction.TransactionStatus;
import com.bominvestidor.spring.domain.transaction.TransactionType;
import com.bominvestidor.spring.entity.transaction.PortfolioTransactionEntity;

class IncomeEligibilityCalculatorTests {
	@Test
	void includesBrazilianPurchaseMadeOnRecordDate() {
		assertThat(IncomeEligibilityCalculator.quantityAt(List.of(buy("PETR4", AssetMarket.BR, "10", "2026-01-10")), "PETR4", AssetMarket.BR,
				LocalDate.parse("2026-01-10"), true)).isEqualByComparingTo("10");
	}

	@Test
	void excludesAmericanPurchaseMadeOnExDate() {
		assertThat(IncomeEligibilityCalculator.quantityAt(List.of(buy("AAPL", AssetMarket.US, "10", "2026-01-10")), "AAPL", AssetMarket.US,
				LocalDate.parse("2026-01-10"), false)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	void subtractsSalesThatOccurredBeforeEligibilityDate() {
		assertThat(IncomeEligibilityCalculator.quantityAt(List.of(buy("PETR4", AssetMarket.BR, "10", "2026-01-01"), sell("PETR4", AssetMarket.BR, "3", "2026-01-05")), "PETR4", AssetMarket.BR,
				LocalDate.parse("2026-01-10"), true)).isEqualByComparingTo("7");
	}

	@Test
	void preservesRightWhenAssetIsSoldAfterBrazilianRecordDate() {
		assertThat(IncomeEligibilityCalculator.quantityAt(List.of(buy("PETR4", AssetMarket.BR, "10", "2026-01-01"), sell("PETR4", AssetMarket.BR, "10", "2026-01-11")), "PETR4", AssetMarket.BR,
				LocalDate.parse("2026-01-10"), true)).isEqualByComparingTo("10");
	}

	@Test
	void ignoresPendingAndCancelledTransactions() {
		PortfolioTransactionEntity pending = transaction("PETR4", AssetMarket.BR, TransactionType.BUY, "5", "2026-01-01", TransactionStatus.PENDING);
		PortfolioTransactionEntity cancelled = transaction("PETR4", AssetMarket.BR, TransactionType.BUY, "5", "2026-01-01", TransactionStatus.CANCELLED);
		assertThat(IncomeEligibilityCalculator.quantityAt(List.of(buy("PETR4", AssetMarket.BR, "10", "2026-01-01"), pending, cancelled), "PETR4", AssetMarket.BR,
				LocalDate.parse("2026-01-10"), true)).isEqualByComparingTo("10");
	}

	@Test
	void returnsZeroWhenPositionWasFullySoldBeforeEligibilityDate() {
		assertThat(IncomeEligibilityCalculator.quantityAt(List.of(buy("PETR4", AssetMarket.BR, "10", "2026-01-01"), sell("PETR4", AssetMarket.BR, "10", "2026-01-05")), "PETR4", AssetMarket.BR,
				LocalDate.parse("2026-01-10"), true)).isEqualByComparingTo(BigDecimal.ZERO);
	}

	private PortfolioTransactionEntity buy(String ticker, AssetMarket market, String quantity, String date) { return transaction(ticker, market, TransactionType.BUY, quantity, date); }
	private PortfolioTransactionEntity sell(String ticker, AssetMarket market, String quantity, String date) { return transaction(ticker, market, TransactionType.SELL, quantity, date); }
	private PortfolioTransactionEntity transaction(String ticker, AssetMarket market, TransactionType type, String quantity, String date) {
		return transaction(ticker, market, type, quantity, date, TransactionStatus.EFFECTIVE);
	}
	private PortfolioTransactionEntity transaction(String ticker, AssetMarket market, TransactionType type, String quantity, String date, TransactionStatus status) {
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return new PortfolioTransactionEntity(UUID.randomUUID(), null, ticker, "Asset", market,
				AssetType.STOCK, "BRL", type, status, LocalDate.parse(date), new BigDecimal(quantity), BigDecimal.TEN, BigDecimal.ZERO, now, now);
	}
}
