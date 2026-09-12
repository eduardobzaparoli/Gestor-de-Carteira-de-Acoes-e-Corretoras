CREATE TABLE registered_assets (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    ticker VARCHAR(32) NOT NULL,
    asset_name VARCHAR(200) NOT NULL,
    market VARCHAR(8) NOT NULL,
    asset_type VARCHAR(8) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    last_quote NUMERIC(19, 8) NOT NULL,
    quoted_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_registered_assets PRIMARY KEY (id),
    CONSTRAINT uk_registered_assets_owner_market_ticker UNIQUE (owner_id, market, ticker),
    CONSTRAINT fk_registered_assets_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_registered_assets_owner_market
    ON registered_assets (owner_id, market, ticker);
