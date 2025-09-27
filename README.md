# thomas-cup-kafka
This project is a Spring Boot application (Java, Maven) focused on managing badminton match results with Kafka integration and PostgreSQL persistence. Key features include:
Kafka Producer/Consumer: Implements idempotency, consumer groups, and partition logic for match/game events.
Badminton Rules Enforcement: Validates scores and match structure per official rules.
Database Integration: Uses PostgreSQL (via Docker) to store match results, with Flyway for schema migration.
API Documentation: Swagger is enabled for REST endpoints.
Testing: Includes unit and integration tests for business logic and Kafka flows.
Configuration: Secrets (like DB password) are managed via a .env file for local development.

## Features & Tasks Implemented

- **.gitignore**: Added a suitable .gitignore for a Spring Java Maven project, ignoring IDE files, build output, logs, and OS-specific files.
- **Cleanup**: Removed previously committed files that are now ignored (e.g., .idea/, target/), and committed these changes.
- **Idempotency**: Implemented idempotency in the Kafka producer using a combination of match id and game number, ensuring each game in a match is tracked and updated independently.
- **Consumer Groups & Partitions**: Added a Kafka consumer with multiple consumer groups to demonstrate how messages are distributed across groups and partitions.
- **Badminton Rules Enforcement**: 
  - Scores must be between 0 and 30.
  - A match consists of the best of 3 games.
  - Games 1 and 2 are played to 21 points (cap at 30).
  - Game 3 is played to 15 points (cap at 30).
  - Each game starts at 0-0.
- **Validation**: Added validation logic in the MatchResult model to enforce the above badminton rules.
- **Tests**: Updated and added tests to:
  - Cover idempotency logic (including match id and game number).
  - Ensure correct logging for consumer groups and partitions.
  - Validate badminton scoring rules.
- **Jackson Compatibility**: Ensured the MatchResult model is compatible with JSON serialization/deserialization for Spring controllers.
- **Git Operations**: Committed and pushed all changes to the remote repository.
- **Database Consumer**: Added a dedicated consumer (MatchResultDbConsumer) that listens to two separate Kafka topics:
  - `new-game`: Inserts a new game into the PostgreSQL match_results table (with ON CONFLICT DO NOTHING).
  - `update-score`: Updates the score, winner, and matchDateTime for an existing game in the match_results table.
  - Uses PostgreSQL via Docker (see below for setup).
  - added swagger documentation for the API endpoints.

## How to Run

1. Start the full stack (PostgreSQL, Kafka, monitoring):
   ```sh
   docker compose up -d
   ```

2. Create Kafka topics:
   ```sh
   ./scripts/setup-kafka-topics.sh
   ```

3. Build the project with Maven:
   ```sh
   mvn clean install
   ```

4. Run the Spring Boot application:
   ```sh
   mvn spring-boot:run
   ```

5. Run tests:
   ```sh
   mvn test
   ```

## Monitoring & Dashboards

The project includes comprehensive monitoring with Prometheus and Grafana:

### 🏸 Grafana Dashboard
A pre-built **Thomas Cup Badminton Championship Dashboard** is available at `badminton-championship-dashboard.json` featuring:
- **Real-time Kafka metrics**: Consumer activity, message processing rates
- **JVM monitoring**: Heap memory usage and performance
- **HTTP request tracking**: API endpoint activity
- **Badminton-themed UI**: Emojis and sport-specific styling

### Setup Instructions:
1. **Access Grafana**: http://localhost:3001 (admin/admin)
2. **Prometheus datasource**: Auto-configured! The datasource is automatically provisioned with UID `thomas-cup-prometheus`
3. **Import Dashboard**:
   - Go to Dashboards → New → Import
   - Upload `badminton-championship-dashboard.json`
   - The dashboard should automatically use the provisioned Prometheus datasource
   - If needed, change the UID in the JSON from `thomas-cup-prometheus` to match your datasource UID

### Metrics Available:
- `spring_kafka_listener_seconds_count` - Message processing counts
- `spring_kafka_listener_seconds_max` - Consumer processing times  
- `jvm_memory_used_bytes` - JVM memory consumption
- `http_server_requests_seconds_count` - HTTP request rates

## Performance Testing with k6

The project includes comprehensive k6 performance tests for load, spike, and endurance testing:

```sh
# Run performance tests (requires app to be running)
./k6/run-tests.sh load    # Standard load test
./k6/run-tests.sh spike   # Traffic spike test  
./k6/run-tests.sh soak    # 30-minute endurance test
./k6/run-tests.sh all     # Run multiple tests

# Or with Docker directly
docker compose --profile performance run --rm k6
```

See `k6/README.md` for detailed testing documentation.

The project uses Docker Compose to run a local PostgreSQL database and Kafka broker for development. 
The database service is defined in docker-compose.yml, specifying the image, environment variables, ports, and persistent storage. 
Secrets like the database password are managed via a .env file (excluded from source control). 
Flyway handles automatic schema migration, creating and updating tables on startup. 
This setup allows easy local development and testing without manual database installation.

## Notes
- Ensure your Kafka topic `thomas-cup-matches` has multiple partitions to see consumer group/partition behavior.
- Use the API endpoint `/api/match-results` to send match results as JSON.
- The project enforces badminton match rules and idempotency at the producer level.
