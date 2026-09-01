package com.bominvestidor.spring.service.exchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bominvestidor.spring.config.BrokerageIntegrationProperties;
import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.integration.exchange.ExchangeRateStrategy;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTests {
	@Mock private ExchangeRateStrategy strategy;

	@Test
	void reusesTheCachedRateForTheSameRequestedDate() {
		LocalDate requestedDate = LocalDate.of(2026, 8, 29);
		ExchangeRate rate = new ExchangeRate("USD", "BRL", new BigDecimal("5.20"), requestedDate.minusDays(1));
		when(strategy.findClosingRate("USD", "BRL", requestedDate)).thenReturn(Optional.of(rate));
		ExchangeRateCache cache = new ExchangeRateCache(Clock.fixed(Instant.parse("2026-08-29T12:00:00Z"), ZoneOffset.UTC),
				new BrokerageIntegrationProperties());
		ExchangeRateService service = new ExchangeRateService(strategy, cache);

		assertEquals(rate, service.find("USD", "BRL", requestedDate).orElseThrow());
		assertEquals(rate, service.find("USD", "BRL", requestedDate).orElseThrow());
		verify(strategy, times(1)).findClosingRate("USD", "BRL", requestedDate);
	}
}
