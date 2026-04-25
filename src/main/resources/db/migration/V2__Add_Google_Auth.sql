-- Add Google authentication support for users and merchants

-- Add googleId column to users table
ALTER TABLE users ADD COLUMN google_id VARCHAR(255) UNIQUE;

-- Add googleId column to merchants table  
ALTER TABLE merchants ADD COLUMN google_id VARCHAR(255) UNIQUE;

-- Add index for faster lookups
CREATE INDEX idx_users_google_id ON users(google_id);
CREATE INDEX idx_merchants_google_id ON merchants(google_id);

-- Make password nullable for users who sign in with Google only
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE merchants ALTER COLUMN password DROP NOT NULL;
