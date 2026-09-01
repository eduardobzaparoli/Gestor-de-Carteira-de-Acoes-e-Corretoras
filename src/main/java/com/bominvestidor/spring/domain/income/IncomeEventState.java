package com.bominvestidor.spring.domain.income;

public interface IncomeEventState {
	IncomeEventStatus status();
	boolean canCancel();
}
