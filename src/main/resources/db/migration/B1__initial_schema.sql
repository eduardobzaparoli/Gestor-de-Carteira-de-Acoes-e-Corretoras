CREATE TABLE users (
    id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE brokerages (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    nickname_key VARCHAR(100) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    legal_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    registration_status VARCHAR(100) NOT NULL,
    cvm_participant_category VARCHAR(150) NOT NULL,
    cep VARCHAR(8) NOT NULL,
    street VARCHAR(150) NOT NULL,
    neighborhood VARCHAR(100) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(100),
    city VARCHAR(150) NOT NULL,
    state VARCHAR(2) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_brokerages PRIMARY KEY (id),
    CONSTRAINT uk_brokerages_owner_cnpj UNIQUE (owner_id, cnpj),
    CONSTRAINT uk_brokerages_owner_nickname_key UNIQUE (owner_id, nickname_key),
    CONSTRAINT fk_brokerages_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE portfolios (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    brokerage_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    name_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_portfolios PRIMARY KEY (id),
    CONSTRAINT uk_portfolios_owner_name_key UNIQUE (owner_id, name_key),
    CONSTRAINT fk_portfolios_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_portfolios_brokerage FOREIGN KEY (brokerage_id) REFERENCES brokerages (id)
);

CREATE TABLE portfolio_transactions (
    id UUID NOT NULL,
    portfolio_id UUID NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    asset_name VARCHAR(200) NOT NULL,
    market VARCHAR(8) NOT NULL,
    asset_type VARCHAR(8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    type VARCHAR(8) NOT NULL,
    status VARCHAR(12) NOT NULL,
    transaction_date DATE NOT NULL,
    quantity NUMERIC(19, 8) NOT NULL,
    unit_price NUMERIC(19, 8) NOT NULL,
    costs NUMERIC(19, 8) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_portfolio_transactions PRIMARY KEY (id),
    CONSTRAINT fk_portfolio_transactions_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
);

CREATE TABLE portfolio_income_events (
    id UUID NOT NULL,
    portfolio_id UUID NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    asset_name VARCHAR(200) NOT NULL,
    market VARCHAR(8) NOT NULL,
    asset_type VARCHAR(8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    type VARCHAR(24) NOT NULL,
    source VARCHAR(20) NOT NULL,
    event_key VARCHAR(512) NOT NULL,
    status VARCHAR(12) NOT NULL,
    eligibility_date DATE,
    payment_date DATE NOT NULL,
    eligible_quantity NUMERIC(19, 8),
    unit_amount NUMERIC(19, 8),
    expected_amount NUMERIC(19, 8),
    received_amount NUMERIC(19, 8) NOT NULL,
    adjustment_reason VARCHAR(500),
    notes VARCHAR(500),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_portfolio_income_events PRIMARY KEY (id),
    CONSTRAINT uk_portfolio_income_events_source_key UNIQUE (portfolio_id, source, event_key),
    CONSTRAINT fk_portfolio_income_events_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios (id)
);
