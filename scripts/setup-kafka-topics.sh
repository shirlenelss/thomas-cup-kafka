#!/bin/bash

# Setup Kafka topics for Thomas Cup Kafka application
# Run this after starting the Kafka broker

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Setting up Kafka topics for Thomas Cup...${NC}"

# Wait for Kafka to be ready
echo -e "${YELLOW}Waiting for Kafka to be ready...${NC}"
sleep 10

# Create topics with appropriate partitions and replication
docker exec kafka kafka-topics --create \
  --topic thomas-cup-matches \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

docker exec kafka kafka-topics --create \
  --topic new-game \
  --bootstrap-server localhost:9092 \
  --partitions 2 \
  --replication-factor 1 \
  --if-not-exists

docker exec kafka kafka-topics --create \
  --topic update-score \
  --bootstrap-server localhost:9092 \
  --partitions 2 \
  --replication-factor 1 \
  --if-not-exists

echo -e "${GREEN}✅ Kafka topics created successfully!${NC}"

# List topics to verify
echo -e "${YELLOW}Current topics:${NC}"
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

echo -e "${GREEN}Kafka setup complete. Ready for match results processing!${NC}"