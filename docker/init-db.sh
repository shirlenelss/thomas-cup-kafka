#!/bin/bash
set -e

# Create the user and database if they don't exist
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create user if not exists (PostgreSQL 15+ syntax)
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'thomas_cup_user') THEN
            CREATE USER thomas_cup_user WITH PASSWORD '$DB_PASSWORD';
        END IF;
    END
    \$\$;
    
    -- Create database if not exists
    SELECT 'CREATE DATABASE thomas_cup_dev OWNER thomas_cup_user'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'thomas_cup_dev')\gexec
    
    -- Grant all privileges
    GRANT ALL PRIVILEGES ON DATABASE thomas_cup_dev TO thomas_cup_user;
    
    -- Connect to the database and set up schema permissions
    \c thomas_cup_dev
    GRANT ALL ON SCHEMA public TO thomas_cup_user;
EOSQL

echo "Database and user setup complete!"