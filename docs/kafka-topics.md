# Kafka Topics Reference

This document describes the Kafka topics used in the Thomas Cup application and their specific purposes.

## Topics Overview

### `thomas-cup-matches` (3 partitions)
- **Purpose**: Main topic for match result events
- **Consumers**: Multiple consumer groups for different processing pipelines
  - `Group-1`: General processing and logging
  - `Group-2`: Alternative processing pipeline
  - `db-writer-group`: Database persistence
- **Message Format**: MatchResult JSON
- **Usage**: Primary event stream for all match updates

### `new-game` (2 partitions)
- **Purpose**: Database insert operations for new games
- **Consumers**: `db-writer-group` (MatchResultDbConsumer)
- **Database Operation**: `INSERT ... ON CONFLICT DO NOTHING`
- **Usage**: Ensures idempotent game creation with composite key (matchId + gameNumber)

### `update-score` (2 partitions)
- **Purpose**: Score updates for existing games
- **Consumers**: `db-writer-group` (MatchResultDbConsumer)
- **Database Operation**: `UPDATE` existing records
- **Usage**: Real-time score updates during match progression

## Topic Configuration

All topics are configured with:
- **Replication Factor**: 1 (suitable for local development)
- **Cleanup Policy**: Default (delete)
- **Retention**: Default (7 days)

## Consumer Groups

### `db-writer-group`
- Listens to all three topics
- Handles database persistence
- Uses different SQL operations per topic

### `Group-1` and `Group-2`
- Listen only to `thomas-cup-matches`
- Used for demonstration of partition distribution
- Primarily for logging and monitoring

## Message Flow

```
API Request → MatchResultProducer → thomas-cup-matches → Multiple Consumers
                                 ↓
                              new-game → Database INSERT
                                 ↓  
                           update-score → Database UPDATE
```

## Monitoring

- **Kafka Manager/UI**: Access via localhost:9092
- **Prometheus Metrics**: Kafka JMX metrics on port 9101
- **Consumer Lag**: Monitor via application metrics

## Development Commands

```bash
# List topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Describe topic
docker exec kafka kafka-topics --describe --topic thomas-cup-matches --bootstrap-server localhost:9092

# Console consumer (for debugging)
docker exec kafka kafka-console-consumer --topic thomas-cup-matches --from-beginning --bootstrap-server localhost:9092

# Check consumer groups
docker exec kafka kafka-consumer-groups --list --bootstrap-server localhost:9092
```