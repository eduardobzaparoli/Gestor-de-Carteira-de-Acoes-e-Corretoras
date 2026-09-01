package com.bominvestidor.spring.domain.income;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeProviderEvent(String eventKey, IncomeEventType type, BigDecimal unitAmount,
		LocalDate eligibilityDate, LocalDate paymentDate, IncomeEventSource source) { }
