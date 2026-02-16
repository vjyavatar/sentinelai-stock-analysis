-- V1__initial_schema.sql
-- Initial database schema for Sentinel Stock Analysis Platform

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    role VARCHAR(20) DEFAULT 'USER',
    verification_token VARCHAR(255),
    reset_token VARCHAR(255),
    reset_token_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Stock Reports table
CREATE TABLE stock_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ticker VARCHAR(20) NOT NULL,
    company_name VARCHAR(255),
    current_price DECIMAL(12, 2),
    currency VARCHAR(10),
    market_cap BIGINT,
    analysis_text TEXT,
    live_data JSONB,
    recommendation VARCHAR(10),
    confidence_score DECIMAL(5, 2),
    is_shared BOOLEAN DEFAULT false,
    share_token VARCHAR(64) UNIQUE,
    view_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_stock_reports_user_id ON stock_reports(user_id);
CREATE INDEX idx_stock_reports_ticker ON stock_reports(ticker);
CREATE INDEX idx_stock_reports_user_ticker ON stock_reports(user_id, ticker);
CREATE INDEX idx_stock_reports_created_at ON stock_reports(created_at);
CREATE INDEX idx_stock_reports_share_token ON stock_reports(share_token);

-- JSONB index for querying live_data
CREATE INDEX idx_stock_reports_live_data ON stock_reports USING gin(live_data);

-- Watchlists table
CREATE TABLE watchlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ticker VARCHAR(20) NOT NULL,
    company_name VARCHAR(255),
    added_price DECIMAL(12, 2),
    target_price DECIMAL(12, 2),
    stop_loss DECIMAL(12, 2),
    notes VARCHAR(500),
    alert_enabled BOOLEAN DEFAULT false,
    alert_price DECIMAL(12, 2),
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_checked TIMESTAMP,
    UNIQUE(user_id, ticker)
);

CREATE INDEX idx_watchlists_user_id ON watchlists(user_id);
CREATE INDEX idx_watchlists_ticker ON watchlists(ticker);

-- Portfolios table
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ticker VARCHAR(20) NOT NULL,
    company_name VARCHAR(255),
    shares DECIMAL(10, 4) NOT NULL,
    average_buy_price DECIMAL(12, 2) NOT NULL,
    current_price DECIMAL(12, 2),
    total_invested DECIMAL(12, 2),
    current_value DECIMAL(12, 2),
    profit_loss DECIMAL(12, 2),
    profit_loss_pct DECIMAL(6, 2),
    purchased_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP,
    UNIQUE(user_id, ticker)
);

CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_portfolios_ticker ON portfolios(ticker);

-- Comments for documentation
COMMENT ON TABLE users IS 'Registered users of the platform';
COMMENT ON TABLE stock_reports IS 'AI-generated stock analysis reports';
COMMENT ON TABLE watchlists IS 'User watchlists for monitoring stocks';
COMMENT ON TABLE portfolios IS 'User portfolio holdings and performance tracking';

COMMENT ON COLUMN stock_reports.live_data IS 'JSONB field storing complete Python service response including live market data and AI analysis';
COMMENT ON COLUMN stock_reports.share_token IS 'Unique token for sharing reports publicly';
