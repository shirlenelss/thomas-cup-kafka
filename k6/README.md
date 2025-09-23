# K6 Performance Testing for Thomas Cup Kafka

This directory contains k6 performance tests for the Thomas Cup Kafka application, focusing on load testing the match results API and validating Kafka message processing under various load conditions.

## Test Scripts

### 1. `load-test.js` - Standard Load Test
- **Purpose**: General performance testing with realistic badminton match data
- **Load Pattern**: Gradual ramp-up to 50 concurrent users
- **Duration**: ~4 minutes
- **Features**:
  - Realistic badminton scoring (21 points for games 1&2, 15 for game 3)
  - Proper deuce handling (scores can go up to 30)
  - Custom metrics for Kafka processing
  - Match result validation

### 2. `spike-test.js` - Traffic Spike Test  
- **Purpose**: Test system behavior under sudden traffic bursts
- **Load Pattern**: Spike from 1 to 100 users instantly
- **Duration**: ~1 minute
- **Use Case**: Simulates viral tournament moments or system stress

### 3. `soak-test.js` - Endurance Test
- **Purpose**: Long-term stability testing
- **Load Pattern**: Sustained 10 users for 30 minutes  
- **Duration**: 40 minutes total
- **Use Case**: Detect memory leaks and performance degradation

## Running Tests

### Prerequisites
1. Start the application stack:
   ```bash
   docker compose up -d postgres prometheus grafana
   mvn spring-boot:run
   ```

2. Ensure the API is accessible at `http://localhost:8080`

### Run with Docker (Recommended)
```bash
# Standard load test
docker compose --profile performance run --rm k6

# Spike test
docker compose --profile performance run --rm k6 run /scripts/spike-test.js

# Soak test (30 minutes)
docker compose --profile performance run --rm k6 run /scripts/soak-test.js

# Custom parameters
docker compose --profile performance run --rm k6 run -e USERS=20 -e DURATION=2m /scripts/load-test.js
```

### Run with Local k6
```bash
# Install k6: https://k6.io/docs/get-started/installation/

# Run tests
k6 run k6/load-test.js
k6 run k6/spike-test.js  
k6 run k6/soak-test.js
```

## Test Data Generation

The tests generate realistic badminton match data:
- **Team Names**: Mix of country names and generic teams
- **Scoring Rules**: Enforces badminton scoring (21/15 point limits, deuce rules)
- **Match Progression**: Simulates 3-game match structure
- **Timing**: Realistic delays between match updates

## Monitoring During Tests

1. **Application Metrics**: `http://localhost:9090` (Prometheus)
2. **Dashboards**: `http://localhost:3001` (Grafana)  
3. **API Health**: `http://localhost:8080/actuator/health`
4. **Kafka Topics**: Monitor message throughput and consumer lag
5. **Database**: Check PostgreSQL for inserted/updated match results

## Key Metrics to Watch

### K6 Metrics
- `http_req_duration`: Response times (target: p95 < 500ms)
- `http_req_failed`: Error rate (target: < 5%)
- `badminton_matches_sent`: Custom counter for sent matches
- `kafka_errors`: Custom metric for Kafka-specific failures

### Application Metrics  
- Kafka consumer lag
- Database connection pool usage
- JVM memory/GC metrics
- HTTP request throughput

## Interpreting Results

### Good Performance
- Response times: p95 < 500ms, p99 < 1000ms
- Error rate: < 2%
- Kafka messages processed without backlog
- Stable memory usage during soak test

### Investigation Needed
- Response times: p95 > 1000ms
- Error rate: > 5%  
- Kafka consumer lag increasing
- Memory usage trending upward
- Database connection timeouts

## Troubleshooting

### Common Issues
1. **Connection Refused**: Ensure Spring Boot app is running on port 8080
2. **High Error Rates**: Check Kafka broker status and database connectivity  
3. **Slow Response Times**: Monitor database query performance and Kafka throughput
4. **Memory Issues**: Check for proper connection cleanup and object disposal

### Docker Network Issues
If running k6 from Docker but app locally, use:
- `host.docker.internal:8080` (included in scripts)
- Or run everything in Docker with proper networking