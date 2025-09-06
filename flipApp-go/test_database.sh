#!/bin/bash

# Test script to verify PostgreSQL database implementation

echo "Testing PostgreSQL database implementation..."

# Start the services
docker-compose up -d postgres

# Wait for PostgreSQL to be ready
echo "Waiting for PostgreSQL to be ready..."
sleep 10

# Run database migrations
echo "Running database migrations..."
docker-compose exec postgres psql -U user -d flipapp -c "
-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    profile_type VARCHAR(20) NOT NULL DEFAULT 'PERSON',
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company_name VARCHAR(255),
    avatar_url VARCHAR(500),
    phone_number VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create refresh_tokens table for auth service
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(500) UNIQUE NOT NULL,
    user_id VARCHAR(255) NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Create properties table
CREATE TABLE IF NOT EXISTS properties (
    id SERIAL PRIMARY KEY,
    property_id VARCHAR(255) UNIQUE NOT NULL,
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    zip_code VARCHAR(20) NOT NULL,
    square_footage INTEGER,
    bedrooms FLOAT,
    bathrooms FLOAT,
    lot_size FLOAT,
    year_built INTEGER,
    property_type VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_properties_property_id ON properties(property_id);
CREATE INDEX IF NOT EXISTS idx_properties_city_state ON properties(city, state);
"

# Test database connection
echo "Testing database connection..."
docker-compose exec postgres psql -U user -d flipapp -c "SELECT 'Database connection successful' as result;"

# Test tables exist
echo "Testing tables exist..."
docker-compose exec postgres psql -U user -d flipapp -c "
SELECT 'Users table: ' || CASE WHEN EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'users') THEN 'OK' ELSE 'MISSING' END;
SELECT 'Refresh tokens table: ' || CASE WHEN EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'refresh_tokens') THEN 'OK' ELSE 'MISSING' END;
SELECT 'Properties table: ' || CASE WHEN EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'properties') THEN 'OK' ELSE 'MISSING' END;
"

echo "Database setup completed successfully!"
echo "You can now start the services: docker-compose up -d"
