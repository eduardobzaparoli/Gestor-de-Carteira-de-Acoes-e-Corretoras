package com.bominvestidor.spring.support;

import com.bominvestidor.spring.repository.brokerage.BrokerageRepository;
import com.bominvestidor.spring.repository.income.PortfolioIncomeEventRepository;
import com.bominvestidor.spring.repository.portfolio.PortfolioRepository;
import com.bominvestidor.spring.repository.transaction.PortfolioTransactionRepository;
import com.bominvestidor.spring.repository.user.UserRepository;

public final class IntegrationTestDataCleaner {

	private final PortfolioIncomeEventRepository incomeEvents;
	private final PortfolioTransactionRepository transactions;
	private final PortfolioRepository portfolios;
	private final BrokerageRepository brokerages;
	private final UserRepository users;

	public IntegrationTestDataCleaner(PortfolioIncomeEventRepository incomeEvents, PortfolioTransactionRepository transactions,
			PortfolioRepository portfolios, BrokerageRepository brokerages, UserRepository users) {
		this.incomeEvents = incomeEvents;
		this.transactions = transactions;
		this.portfolios = portfolios;
		this.brokerages = brokerages;
		this.users = users;
	}

	public void clear() {
		incomeEvents.deleteAll();
		transactions.deleteAll();
		portfolios.deleteAll();
		brokerages.deleteAll();
		users.deleteAll();
	}
}
