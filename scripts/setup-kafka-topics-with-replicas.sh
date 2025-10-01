#!/bin/bash

# Setup Kafka topics with replication for Thomas Cup application
# This script creates topics with replication factor 3 and min ISR 2

echo "Setting up Kafka topics with replication..."

# Wait for all Kafka brokers to be ready
echo "Waiting for Kafka brokers to be ready..."
sleep 10

# Create topics with replication factor 3 and min ISR 2
docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094 \
  --topic thomas-cup-matches \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config cleanup.policy=delete \
  --config retention.ms=604800000

docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094 \
  --topic new-game \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config cleanup.policy=delete \
  --config retention.ms=604800000

docker exec kafka1 kafka-topics --create \
  --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094 \
  --topic update-score \
  --partitions 3 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --config cleanup.policy=delete \
  --config retention.ms=604800000

echo "Topics created successfully with replication factor 3!"

# List topics to verify
echo "Verifying topics:"
docker exec kafka1 kafka-topics --list --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094

# Show topic details
echo "Topic details:"
docker exec kafka1 kafka-topics --describe --bootstrap-server kafka1:29092,kafka2:29093,kafka3:29094

echo "Kafka topics setup completed!"