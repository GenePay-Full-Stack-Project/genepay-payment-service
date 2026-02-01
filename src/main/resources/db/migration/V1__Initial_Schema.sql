-- GenePay Payment Service - Initial Schema Migration
-- Version: V1
-- Description: Creates initial schema with users, merchants, admins, and cards tables

-- Create users table
CREATE TABLE users
(
    id                         BIGSERIAL PRIMARY KEY,
    email                      VARCHAR(255) NOT NULL UNIQUE,
    password                   VARCHAR(255) NOT NULL,
    full_name                  VARCHAR(255) NOT NULL,
    nic_number                 VARCHAR(50)  NOT NULL UNIQUE,
    phone_number               VARCHAR(20)  NOT NULL,
    face_id                    VARCHAR(255) UNIQUE,
    balance                    NUMERIC(10, 2) DEFAULT 0.00,
    status                     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified             BOOLEAN      NOT NULL DEFAULT FALSE,
    email_verification_code    VARCHAR(10),
    email_verification_expiry  TIMESTAMP,
    face_enrolled              BOOLEAN      NOT NULL DEFAULT FALSE,
    card_linked                BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts      INTEGER               DEFAULT 0,
    locked_until               TIMESTAMP,
    created_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at              TIMESTAMP,
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE', 'DELETED'))
);

-- Create merchants table
CREATE TABLE merchants
(
    id                    BIGSERIAL PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    business_name         VARCHAR(255) NOT NULL,
    owner_name            VARCHAR(255),
    phone_number          VARCHAR(20) UNIQUE,
    business_address      TEXT,
    business_type         VARCHAR(100),
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    card_linked           BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER               DEFAULT 0,
    locked_until          TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at         TIMESTAMP,
    CONSTRAINT chk_merchant_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE', 'DELETED'))
);

-- Create admins table
CREATE TABLE admins
(
    id                    BIGSERIAL PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    role                  VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    status                VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INTEGER               DEFAULT 0,
    locked_until          TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at         TIMESTAMP,
    CONSTRAINT chk_admin_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'SUPPORT')),
    CONSTRAINT chk_admin_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

-- Create cards table
CREATE TABLE cards
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT REFERENCES users (id) ON DELETE CASCADE,
    merchant_id    BIGINT REFERENCES merchants (id) ON DELETE CASCADE,
    payment_token  VARCHAR(255) NOT NULL UNIQUE,
    card_last4     VARCHAR(4)   NOT NULL,
    card_brand     VARCHAR(50),
    expiry_month   VARCHAR(2),
    expiry_year    VARCHAR(4),
    is_default     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    nickname       VARCHAR(100),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at   TIMESTAMP,
    CONSTRAINT chk_card_owner CHECK (
        (user_id IS NOT NULL AND merchant_id IS NULL) OR
        (user_id IS NULL AND merchant_id IS NOT NULL)
        )
);

-- Create transactions table
CREATE TABLE transactions
(
    id                      BIGSERIAL PRIMARY KEY,
    transaction_id          VARCHAR(255) NOT NULL UNIQUE,
    user_id                 BIGINT REFERENCES users (id) ON DELETE SET NULL,
    merchant_id             BIGINT       NOT NULL REFERENCES merchants (id) ON DELETE CASCADE,
    amount                  NUMERIC(10, 2) NOT NULL,
    currency                VARCHAR(3)   NOT NULL DEFAULT 'LKR',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    type                    VARCHAR(20)  NOT NULL DEFAULT 'PAYMENT',
    banking_transaction_id  VARCHAR(255),
    description             VARCHAR(500),
    failure_reason          VARCHAR(500),
    metadata                TEXT,
    biometric_verified      BOOLEAN      NOT NULL DEFAULT FALSE,
    face_verification_id    VARCHAR(255),
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at            TIMESTAMP,
    CONSTRAINT chk_transaction_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT chk_transaction_type CHECK (type IN ('PAYMENT', 'REFUND', 'ADJUSTMENT'))
);

-- Create indexes for better query performance
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_nic_number ON users (nic_number);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_face_id ON users (face_id);

CREATE INDEX idx_merchants_email ON merchants (email);
CREATE INDEX idx_merchants_status ON merchants (status);
CREATE INDEX idx_merchants_phone_number ON merchants (phone_number);

CREATE INDEX idx_admins_email ON admins (email);
CREATE INDEX idx_admins_role ON admins (role);
CREATE INDEX idx_admins_status ON admins (status);

CREATE INDEX idx_cards_user_id ON cards (user_id);
CREATE INDEX idx_cards_merchant_id ON cards (merchant_id);
CREATE INDEX idx_cards_payment_token ON cards (payment_token);
CREATE INDEX idx_cards_is_default ON cards (is_default);
CREATE INDEX idx_cards_is_active ON cards (is_active);

CREATE INDEX idx_transactions_transaction_id ON transactions (transaction_id);
CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_merchant_id ON transactions (merchant_id);
CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_type ON transactions (type);
CREATE INDEX idx_transactions_banking_transaction_id ON transactions (banking_transaction_id);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_user_status ON transactions (user_id, status);
CREATE INDEX idx_transactions_merchant_status ON transactions (merchant_id, status);
CREATE INDEX idx_transactions_user_created ON transactions (user_id, created_at);
CREATE INDEX idx_transactions_merchant_created ON transactions (merchant_id, created_at);

-- Add comments for documentation
COMMENT ON TABLE users IS 'Stores user account information for the GenePay platform';
COMMENT ON TABLE merchants IS 'Stores merchant account information for accepting payments';
COMMENT ON TABLE admins IS 'Stores admin user accounts for platform management';
COMMENT ON TABLE cards IS 'Stores linked payment cards for users and merchants';
COMMENT ON TABLE transactions IS 'Stores payment transaction records for GenePay platform';

COMMENT ON COLUMN cards.payment_token IS 'Banking system payment token (UUID) for secure transactions';
COMMENT ON COLUMN cards.card_last4 IS 'Last 4 digits of card number for display purposes';
COMMENT ON COLUMN users.face_id IS 'Reference to biometric service for facial recognition';
COMMENT ON COLUMN users.balance IS 'User wallet balance in platform currency';
COMMENT ON COLUMN transactions.transaction_id IS 'Unique UUID identifier for the transaction';
COMMENT ON COLUMN transactions.user_id IS 'Reference to user making payment (nullable until biometric verification)';
COMMENT ON COLUMN transactions.merchant_id IS 'Reference to merchant receiving payment';
COMMENT ON COLUMN transactions.banking_transaction_id IS 'Reference to banking system transaction ID';
COMMENT ON COLUMN transactions.biometric_verified IS 'Indicates if transaction was verified via biometric authentication';
COMMENT ON COLUMN transactions.face_verification_id IS 'Reference to biometric service verification session';
COMMENT ON COLUMN transactions.metadata IS 'JSON string for additional transaction data';
