# Thomas Cup Kafka - Complete Run & Test Guide

## 🚀 Quick Start (First Time Setup)

### 1. Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- (Optional) k6 for performance testing

### 2. Start Infrastructure
```bash
# Start all services (PostgreSQL, Kafka, Zookeeper, Prometheus, Grafana)
docker compose up -d

# Verify services are running
docker compose ps
```

### 3. Setup Kafka Topics
```bash
# Create required topics (thomas-cup-matches, new-game, update-score)
./scripts/setup-kafka-topics.sh

# Verify topics were created
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 4. Build & Run Application
```bash
# Build with Avro schema generation
mvn clean install

# Set database password environment variable
export SPRING_DATASOURCE_PASSWORD=your_password_here

# Run the Spring Boot application
mvn spring-boot:run
```

### 5. Verify Application is Running
```bash
# Health check
curl http://localhost:8080/actuator/health

# Should return: {"status":"UP"}
```

## 🧪 Testing Guide

### Unit & Integration Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=MatchResultConsumerTest

# Run integration tests only
mvn test -Dtest=*IntegrationTest
```

### Manual API Testing

#### Using curl:
```bash
# Send a match result
curl -X POST http://localhost:8080/api/match-results \
  -H "Content-Type: application/json" \
  -d '{
    "id": "match-1",
    "teamA": "Malaysia",
    "teamB": "Indonesia", 
    "teamAScore": 21,
    "teamBScore": 19,
    "winner": "Malaysia",
    "matchDateTime": "2025-09-23T15:30:00",
    "gameNumber": 1
  }'
```

#### Using the HTTP file:
Open `src/test/requests.http` in VS Code and click "Send Request"

### Kafka Message Verification
```bash
# Monitor messages on main topic
docker exec kafka kafka-console-consumer \
  --topic thomas-cup-matches \
  --from-beginning \
  --bootstrap-server localhost:9092

# Check consumer groups
docker exec kafka kafka-consumer-groups \
  --list --bootstrap-server localhost:9092

# Check consumer lag
docker exec kafka kafka-consumer-groups \
  --describe --group db-writer-group \
  --bootstrap-server localhost:9092
```

### Database Verification
```bash
# Connect to PostgreSQL
docker exec -it $(docker compose ps -q postgres) psql -U thomas_cup_user -d thomas_cup_dev

# Check match results
SELECT * FROM match_results ORDER BY matchdatetime DESC;

# Exit psql
\q
```

## ⚡ Performance Testing with k6

### Basic Load Test
```bash
# Ensure app is running first, then:
./k6/run-tests.sh load
```

### All Test Types
```bash
./k6/run-tests.sh load    # Standard load test (4 min)
./k6/run-tests.sh spike   # Traffic spike test (1 min)
./k6/run-tests.sh soak    # Endurance test (40 min)
./k6/run-tests.sh all     # Run load + spike tests
```

### Custom k6 Test
```bash
# Run with custom parameters
docker compose --profile performance run --rm k6 run \
  -e VUS=50 -e DURATION=5m /scripts/load-test.js
```

## 📊 Monitoring & Observability

### Application Metrics
- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### Infrastructure Monitoring
- **Prometheus UI**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Swagger API Docs**: http://localhost:8080/swagger-ui.html

### Key Metrics to Monitor
```bash
# Application metrics
curl http://localhost:8080/actuator/metrics/kafka.consumer.records-consumed-total
curl http://localhost:8080/actuator/metrics/kafka.producer.record-send-total

# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Application Won't Start
```bash
# Check if database is accessible
docker exec postgres pg_isready -U thomas_cup_user

# Check Kafka connectivity
docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

#### 2. Kafka Connection Issues
```bash
# Restart Kafka services
docker compose restart zookeeper kafka

# Recreate topics if needed
./scripts/setup-kafka-topics.sh
```

#### 3. Database Connection Issues
```bash
# Check environment variable
echo $SPRING_DATASOURCE_PASSWORD

# Verify PostgreSQL is running
docker compose logs postgres
```

#### 4. k6 Tests Failing
```bash
# Verify app health
curl http://localhost:8080/actuator/health

# Check Docker networking
docker network ls
```

### Logs & Debugging
```bash
# Application logs
mvn spring-boot:run | grep -E "(ERROR|WARN|kafka|postgres)"

# Docker service logs
docker compose logs postgres
docker compose logs kafka
docker compose logs zookeeper

# Follow real-time logs
docker compose logs -f kafka
```

## 🔄 Development Workflow

### Daily Development
```bash
# 1. Start infrastructure (if not already running)
docker compose up -d

# 2. Run application with live reload
mvn spring-boot:run

# 3. Make code changes

# 4. Run tests to verify changes
mvn test -Dtest=YourTestClass

# 5. Test API manually or with k6
```

### Before Committing
```bash
# Run full test suite
mvn clean test

# Performance regression test
./k6/run-tests.sh load

# Check application health
curl http://localhost:8080/actuator/health
```

### Clean Restart
```bash
# Stop everything
docker compose down
mvn spring-boot:stop (if running)

# Clean build
mvn clean install

# Fresh start
docker compose up -d
./scripts/setup-kafka-topics.sh
mvn spring-boot:run
```

## 📝 Test Scenarios

### Badminton Match Flow Testing
1. **New Match**: Send game 1 result
2. **Game Progression**: Send games 2 and 3 results with same match ID
3. **Score Updates**: Send incremental score updates
4. **Validation**: Check database for complete match history

### Idempotency Testing
```bash
# Send same match result twice - should not create duplicates
curl -X POST http://localhost:8080/api/match-results -H "Content-Type: application/json" -d '{"id":"dup-test","teamA":"A","teamB":"B","teamAScore":21,"teamBScore":19,"winner":"A","matchDateTime":"2025-09-23T15:30:00","gameNumber":1}'

# Send again - check database for single record
curl -X POST http://localhost:8080/api/match-results -H "Content-Type: application/json" -d '{"id":"dup-test","teamA":"A","teamB":"B","teamAScore":21,"teamBScore":19,"winner":"A","matchDateTime":"2025-09-23T15:30:00","gameNumber":1}'
```

This guide covers everything you need to run, test, and monitor your Thomas Cup Kafka application effectively!