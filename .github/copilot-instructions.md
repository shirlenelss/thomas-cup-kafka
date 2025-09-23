# Thomas Cup Kafka - AI Agent Instructions

## Architecture Overview
This is a Spring Boot 3.x application implementing badminton match result processing via Kafka with PostgreSQL persistence. The system follows an event-driven architecture with separate producers/consumers for different data flows.

### Core Domain Model
- **MatchResult**: Legacy model (deprecated) for single game results
- **MatchHead + MatchScores**: Current approach using Avro schemas in `src/main/avro/`
- **Badminton Rules**: Games 1&2 play to 21 points, Game 3 to 15 points, all capped at 30

### Multi-Topic Kafka Pattern
The system uses topic specialization:
- `thomas-cup-matches`: Main topic for match events with consumer groups
- `new-game`: Database inserts with conflict handling (`ON CONFLICT DO NOTHING`)
- `update-score`: Database updates for existing games

## Key Development Patterns

### Idempotency Implementation
Match results use composite keys: `matchId + gameNumber`. See `MatchResultDbConsumer.saveNewGameToDb()` for the PostgreSQL `ON CONFLICT` pattern that prevents duplicate game entries.

### Testing Strategy
- **Integration Tests**: Use `@EmbeddedKafka` with `EmbeddedKafkaBroker` for full Kafka flow testing
- **Consumer Testing**: Mock loggers via inheritance pattern (see `MatchResultConsumerTest.MatchResultConsumerWithLogger`)
- **Async Testing**: Use Awaitility (`await().atMost()`) for Kafka message consumption verification

### Environment Setup
```bash
# Start PostgreSQL + Kafka + monitoring stack
docker compose up -d

# Create required Kafka topics
./scripts/setup-kafka-topics.sh

# Build with Avro schema generation
mvn clean install

# Run application
mvn spring-boot:run
```

### Database Patterns
- **Schema Management**: Flyway migrations in `src/main/resources/db/migration/`
- **Composite Primary Keys**: `(id, gameNumber)` prevents duplicate games per match
- **Upsert Logic**: Uses PostgreSQL `MERGE` syntax in `MatchResultDbConsumer`

### Configuration Secrets
- Database password via `${SPRING_DATASOURCE_PASSWORD}` environment variable
- Docker Compose uses `.env` file for `${DB_PASSWORD}`
- Kafka bootstrap servers configurable via `spring.kafka.bootstrap-servers`

### Kafka Consumer Groups
Multiple consumer groups demonstrate partition distribution:
- `db-writer-group`: Database persistence consumers
- `Group-1` and `Group-2`: Separate processing pipelines

### Business Logic Validation
Badminton scoring rules enforced in model setters:
- Scores 0-30 range validation
- Game-specific point limits (21 for games 1&2, 15 for game 3)
- Validation occurs in setter methods, not constructors

### API Documentation
Swagger/OpenAPI enabled automatically at `/swagger-ui.html` via `springdoc-openapi` dependency.

### Performance Testing
- **k6 Integration**: Load testing with realistic badminton match data in `k6/` directory
- **Test Types**: Load tests, spike tests (traffic bursts), and soak tests (30min endurance)
- **Docker Profile**: Use `docker compose --profile performance` for k6 tests
- **Custom Metrics**: Tracks badminton-specific metrics (matches sent, Kafka errors)

## Common Development Tasks

### Adding New Kafka Topics
1. Update consumer with `@KafkaListener(topics = "new-topic")`
2. Add topic to integration tests' `@EmbeddedKafka(topics = {...})`
3. Configure separate consumer groups if needed

### Database Schema Changes
1. Create new Flyway migration: `V{number}__{description}.sql`
2. Use composite keys for game-level data: `PRIMARY KEY (match_id, game_number)`

### Avro Schema Evolution
1. Update `.avsc` files in `src/main/avro/`
2. Regenerate classes via `mvn clean compile` (avro-maven-plugin)
3. Update corresponding Spring models and Kafka configs

### Performance Testing
1. Run k6 tests: `./k6/run-tests.sh [load|spike|soak]`
2. Monitor with Prometheus/Grafana during tests
3. Check database and Kafka for processed results after tests