package com.bominvestidor.spring.domain.transaction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TransactionStatesTests {
	@Test void onlyPendingTransactionsCanBeCancelled() {
		assertTrue(TransactionStates.from(TransactionStatus.PENDING).canCancel());
		assertFalse(TransactionStates.from(TransactionStatus.EFFECTIVE).canCancel());
		assertFalse(TransactionStates.from(TransactionStatus.CANCELLED).canCancel());
	}
}
