package com.bominvestidor.spring.domain.income;

public final class IncomeEventStates {
	private IncomeEventStates() { }
	public static IncomeEventState from(IncomeEventStatus status) {
		return switch (status) {
			case PENDING -> new SimpleState(status, true);
			case EFFECTIVE, CANCELLED -> new SimpleState(status, false);
		};
	}
	private record SimpleState(IncomeEventStatus status, boolean canCancel) implements IncomeEventState { }
}
