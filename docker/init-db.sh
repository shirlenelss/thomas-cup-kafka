#!/bin/bash
set -e

# Thomas Cup Kafka - External Database Setup Script
# Use this script when deploying to external PostgreSQL instances (AWS RDS, Azure Database, etc.)
# For local Docker development, database setup is automatic via docker-compose.yml

echo "🏸 Setting up Thomas Cup Kafka database for external PostgreSQL..."
echo "📋 This script creates the database and user required by the application"
echo ""

# Validate required environment variables
if [ -z "$POSTGRES_HOST" ]; then
    echo "❌ POSTGRES_HOST environment variable is required"
    exit 1
fi

if [ -z "$POSTGRES_ADMIN_USER" ]; then
    echo "❌ POSTGRES_ADMIN_USER environment variable is required"  
    exit 1
fi

if [ -z "$DB_PASSWORD" ]; then
    echo "❌ DB_PASSWORD environment variable is required"
    exit 1
fi

echo "🔌 Connecting to PostgreSQL at $POSTGRES_HOST..."

# Create the user and database if they don't exist
PGPASSWORD=$POSTGRES_ADMIN_PASSWORD psql -h "$POSTGRES_HOST" -U "$POSTGRES_ADMIN_USER" -d postgres <<-EOSQL
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

echo ""
echo "✅ Database and user setup complete!"
echo "📋 Next steps:"
echo "   1. Update application.properties with your PostgreSQL host:"
echo "      spring.datasource.url=jdbc:postgresql://$POSTGRES_HOST:5432/thomas_cup_dev"
echo "   2. Set environment variable: export SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD"
echo "   3. Run your Spring Boot application - Flyway will create tables automatically"
echo ""
echo "🎯 Application is now ready for deployment!"