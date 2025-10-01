# K6 Performance Testing for Thomas Cup Kafka

This directory contains k6 performance tests for the Thomas Cup Kafka application, focusing on load testing the match results API and validating Kafka message processing under various load conditions.

## Test Scripts

### 1. `load-test.js` - Enhanced Load Test
- **Purpose**: Comprehensive load testing with realistic scaling patterns
- **Load Pattern**: Multi-stage ramp from 10→25→50→75→100 users
- **Duration**: ~12 minutes
- **Features**:
  - Advanced badminton match simulation with tournament context
  - Adaptive think time based on server response
  - Detailed performance percentiles (p50, p90, p95, p99)
  - Enhanced response validation and error reporting

### 2. `spike-test.js` - Multi-Phase Spike Test  
- **Purpose**: Test system resilience under multiple traffic spikes
- **Load Pattern**: Baseline→150 users→Recovery→200 users→Recovery
- **Duration**: ~4 minutes
- **Features**:
  - Two different spike intensities to test breaking points
  - Phase-specific performance thresholds
  - Detailed spike recovery analysis
  - Batch request processing during high load

### 3. `soak-test.js` - Extended Endurance Test
- **Purpose**: Long-term stability with varying load patterns
- **Load Pattern**: 5→15→25 users with sustained periods
- **Duration**: ~60 minutes
- **Features**:
  - Periodic health checks every 100 iterations  
  - Phase-aware performance tracking
  - Memory leak detection through sustained load
  - Performance degradation monitoring

### 4. `comprehensive-test.js` - Complete Performance Suite
- **Purpose**: All-in-one test combining load, spike, and endurance aspects
- **Load Pattern**: Warmup→Ramp→Sustained→Spike→Endurance→Final Spike→Cooldown
- **Duration**: ~26 minutes
- **Features**:
  - Tournament simulation with 18 countries and 6 tournament types
  - Match type variety (decisive, close, deuce scenarios)
  - Phase-specific behavior and thresholds
  - Comprehensive metrics including game distribution and response sizes

## Running Tests

### Prerequisites
1. Start the application stack:
   ```bash
   docker compose up -d postgres prometheus grafana
   mvn spring-boot:run
   ```

2. Ensure the API is accessible at `http://localhost:8080`

### Run with Scripts (Recommended)
```bash
# Use the convenient test runner
./k6/run-tests.sh load           # Enhanced load test (12 min)
./k6/run-tests.sh spike          # Multi-phase spike test (4 min)  
./k6/run-tests.sh soak           # Extended endurance test (60 min)
./k6/run-tests.sh comprehensive  # Complete test suite (26 min)
./k6/run-tests.sh all           # Sequential: load + spike + comprehensive

# Or use dev.sh wrapper
./scripts/dev.sh k6             # Runs load test by default
```

### Run with Docker Directly
```bash
# Individual tests
docker compose --profile performance run --rm k6 run /scripts/load-test.js
docker compose --profile performance run --rm k6 run /scripts/spike-test.js
docker compose --profile performance run --rm k6 run /scripts/soak-test.js
docker compose --profile performance run --rm k6 run /scripts/comprehensive-test.js
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
- `http_req_duration`: Response times (target: p50<250ms, p95<1000ms)
- `http_req_failed`: Error rate (target: < 2%)  
- `badminton_matches_sent`: Total matches processed
- `kafka_errors`: Kafka-specific failure rate
- `match_processing_duration`: End-to-end processing time
- `api_response_size_bytes`: Response payload efficiency
- `games_by_number`: Distribution of games 1, 2, and 3
- `concurrent_users`: Active user gauge

### Application Metrics  
- Kafka consumer lag and throughput
- Database connection pool usage and query performance
- JVM memory, GC metrics, and heap usage
- HTTP request rates and error distributions
- Multi-broker Kafka cluster health (3 brokers)

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