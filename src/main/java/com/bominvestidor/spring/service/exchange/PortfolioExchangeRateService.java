package com.bominvestidor.spring.service.exchange;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bominvestidor.spring.domain.exchange.ExchangeRate;
import com.bominvestidor.spring.dto.valuation.ExchangeRateResponse;
import com.bominvestidor.spring.exception.ExchangeRateUnavailableException;
import com.bominvestidor.spring.service.portfolio.PortfolioService;

@Service
public class PortfolioExchangeRateService {

	private final PortfolioService portfolioService;
	private final ExchangeRateService exchangeRateService;

	public PortfolioExchangeRateService(PortfolioService portfolioService, ExchangeRateService exchangeRateService) {
		this.portfolioService = portfolioService;
		this.exchangeRateService = exchangeRateService;
	}

	public ExchangeRateResponse find(UUID ownerId, UUID portfolioId, String sourceCurrency, LocalDate requestedDate) {
		portfolioService.requireOwnedPortfolio(ownerId, portfolioId);
		return find(sourceCurrency, requestedDate);
	}

	public ExchangeRateResponse find(String sourceCurrency, LocalDate requestedDate) {
		String source = sourceCurrency == null ? "" : sourceCurrency.trim().toUpperCase(Locale.ROOT);
		if ("BRL".equals(source)) return new ExchangeRateResponse("BRL", "BRL", java.math.BigDecimal.ONE, requestedDate);
		ExchangeRate rate = exchangeRateService.find(source, "BRL", requestedDate)
				.filter(value -> value.rate() != null && value.rate().signum() > 0
						&& source.equalsIgnoreCase(value.sourceCurrency())
						&& "BRL".equalsIgnoreCase(value.targetCurrency()))
				.orElseThrow(ExchangeRateUnavailableException::new);
		return new ExchangeRateResponse(rate.sourceCurrency(), rate.targetCurrency(), rate.rate(), rate.referenceDate());
	}
}
