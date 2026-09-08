package com.bominvestidor.spring.service.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.dto.valuation.ExchangeRateResponse;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

class PortfolioExchangeRateServiceTests {

	@Test
	void returnsTheRateOnlyAfterCheckingPortfolioOwnership() {
		PortfolioService portfolios = mock(PortfolioService.class);
		ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
		PortfolioExchangeRateService service = new PortfolioExchangeRateService(portfolios, exchangeRates);
		UUID ownerId = UUID.randomUUID();
		UUID portfolioId = UUID.randomUUID();
		LocalDate requestedDate = LocalDate.of(2026, 9, 8);
		ExchangeRate rate = new ExchangeRate("USD", "BRL", new BigDecimal("5.25"), LocalDate.of(2026, 9, 5));
		when(exchangeRates.find("USD", "BRL", requestedDate)).thenReturn(Optional.of(rate));

		ExchangeRateResponse response = service.find(ownerId, portfolioId, "usd", requestedDate);

		verify(portfolios).requireOwnedPortfolio(ownerId, portfolioId);
		assertThat(response.rate()).isEqualByComparingTo("5.25");
		assertThat(response.referenceDate()).isEqualTo(LocalDate.of(2026, 9, 5));
	}
}
