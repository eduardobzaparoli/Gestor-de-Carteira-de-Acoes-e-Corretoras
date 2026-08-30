package com.bominvestidor.spring.domain.transaction;

public interface TransactionState {
	TransactionStatus status();
	boolean canCancel();
}
