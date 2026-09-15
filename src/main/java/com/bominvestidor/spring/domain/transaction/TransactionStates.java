package com.bominvestidor.spring.domain.transaction;

public final class TransactionStates {
	private TransactionStates() { }
	public static TransactionState from(TransactionStatus status) {
		return switch (status) {
			case PENDING -> new SimpleState(status, true);
			case EFFECTIVE, CANCELLED -> new SimpleState(status, false);
		};
	}
	private record SimpleState(TransactionStatus status, boolean canCancel) implements TransactionState { }
}
