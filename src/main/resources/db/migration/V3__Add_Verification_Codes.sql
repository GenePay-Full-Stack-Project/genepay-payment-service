-- Add verification_codes table for stateless email verification

CREATE TABLE verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL,
    expiry_time TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP,
    last_code_sent_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for faster lookups
CREATE INDEX idx_email_type ON verification_codes(email, type);
CREATE INDEX idx_expiry ON verification_codes(expiry_time);

-- Add constraint to check type values
ALTER TABLE verification_codes ADD CONSTRAINT check_verification_type 
    CHECK (type IN ('USER', 'MERCHANT'));
