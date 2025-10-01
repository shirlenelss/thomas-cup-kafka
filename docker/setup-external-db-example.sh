#!/bin/bash

# Thomas Cup Kafka - External Database Setup Example
# Copy this script and modify for your specific environment

# Example 1: AWS RDS PostgreSQL
export POSTGRES_HOST="your-rds-endpoint.amazonaws.com"
export POSTGRES_ADMIN_USER="postgres"
export POSTGRES_ADMIN_PASSWORD="your-admin-password"
export DB_PASSWORD="thomas_cup_secure_password"

# Run the setup script
./docker/init-db.sh

# Example 2: Azure Database for PostgreSQL
export POSTGRES_HOST="your-server.postgres.database.azure.com"
export POSTGRES_ADMIN_USER="your-admin@your-server"
export POSTGRES_ADMIN_PASSWORD="your-admin-password"
export DB_PASSWORD="thomas_cup_secure_password"

# Run the setup script
./docker/init-db.sh

# Example 3: Local PostgreSQL (non-Docker)
export POSTGRES_HOST="localhost"
export POSTGRES_ADMIN_USER="postgres"
export POSTGRES_ADMIN_PASSWORD="postgres"
export DB_PASSWORD="thomas_cup_secure_password"

# Run the setup script
./docker/init-db.sh