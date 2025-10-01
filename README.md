# Thomas Cup Kafka 🏸

A **production-ready Spring Boot application** for managing badminton match results with enterprise-grade Kafka integration and PostgreSQL persistence.

## 🎯 **Key Features**

- **🔄 Multi-Broker Kafka**: 3-broker cluster with replication factor 3 for fault tolerance
- **⚡ Event-Driven Architecture**: Idempotent producers/consumers with partition logic  
- **🏸 Badminton Rules Engine**: Official scoring validation (21/21/15 point system)
- **🗄️ Database Integration**: PostgreSQL with Flyway migrations and automatic setup
- **📊 Production Monitoring**: Prometheus metrics + Grafana dashboards
- **🧪 Comprehensive Testing**: Unit, integration, and k6 performance tests
- **🚀 Easy Deployment**: Automated scripts for local and cloud deployments
- **📚 API Documentation**: OpenAPI/Swagger integration

## 🏗️ **Architecture Overview**

### **Multi-Broker Kafka Cluster**
- **3 Kafka Brokers**: `kafka1:9092`, `kafka2:9093`, `kafka3:9094`
- **Replication Factor**: 3 with minimum in-sync replicas of 2
- **ZooKeeper Coordination**: Single instance managing cluster state
- **Fault Tolerance**: Automatic leader election and partition distribution

### **Event-Driven Data Flow**
- **`thomas-cup-matches`**: Main event stream for match processing
- **`new-game`**: Database inserts with conflict resolution (`ON CONFLICT DO NOTHING`)
- **`update-score`**: Database updates for existing game records
- **Consumer Groups**: Multiple processing pipelines with partition distribution

### **Database Strategy**
- **PostgreSQL 15**: Persistent storage with composite primary keys
- **Flyway Migrations**: Automated schema management and versioning
- **Idempotency**: `(matchId, gameNumber)` prevents duplicate game entries
- **Auto-Setup**: Database/user creation for both local and cloud deployments

### **Badminton Business Rules**
- **Match Structure**: Best of 3 games with official scoring
- **Game 1 & 2**: Play to 21 points (capped at 30)
- **Game 3**: Play to 15 points (capped at 30)
- **Score Validation**: 0-30 range enforcement in model setters

## How to Run

1. Start the full stack (PostgreSQL, Kafka, monitoring):
   ```sh
   docker compose up -d
   ```

2. Create Kafka topics:
   ```sh
   ./scripts/setup-kafka-topics-with-replicas.sh
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

## 🚀 Quick Development Workflow

For faster development, use the convenience script:

```sh
# Quick start - starts all services and sets up topics
./scripts/dev.sh start

# Build and run the application  
./scripts/dev.sh build
./scripts/dev.sh run

# Test the API
./scripts/dev.sh test-api

# Performance testing
./scripts/dev.sh k6
```

See [`scripts/README.md`](scripts/README.md) for all available commands and detailed documentation.

## 🚀 **Production Deployment**

### **Cloud Database Setup**
For AWS RDS, Azure Database, or other external PostgreSQL:

```bash
# 1. Configure database connection
export POSTGRES_HOST="your-db-endpoint.com"
export POSTGRES_ADMIN_USER="postgres"
export POSTGRES_ADMIN_PASSWORD="admin-password"
export DB_PASSWORD="secure-app-password"

# 2. Create database and user
./docker/init-db.sh

# 3. Update application configuration
export SPRING_DATASOURCE_PASSWORD="secure-app-password"
# Update spring.datasource.url in application.properties

# 4. Deploy application - Flyway creates tables automatically
java -jar target/thomas-cup-kafka-*.jar
```

### **Environment Configuration**
```properties
# Production application.properties
spring.datasource.url=jdbc:postgresql://your-host:5432/thomas_cup_dev
spring.kafka.bootstrap-servers=kafka1:9092,kafka2:9092,kafka3:9092
management.endpoints.web.exposure.include=health,metrics,prometheus
```

See [`docker/setup-external-db-example.sh`](docker/setup-external-db-example.sh) for cloud-specific examples.

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

## 🏸 Scripts & Automation

The project includes essential scripts for Kafka setup and badminton match simulation:

```sh
# Setup 3-broker Kafka cluster with replication
./scripts/setup-kafka-topics-with-replicas.sh

# Realistic badminton match simulation  
./scripts/simulate-badminton-match.sh

# Comprehensive system test (all topics/endpoints)
./scripts/full-system-demo.sh
```

The project uses Docker Compose to run a local PostgreSQL database and Kafka broker for development. 
The database service is defined in docker-compose.yml, specifying the image, environment variables, ports, and persistent storage. 
Secrets like the database password are managed via a .env file (excluded from source control). 
Flyway handles automatic schema migration, creating and updating tables on startup. 
This setup allows easy local development and testing without manual database installation.

## Notes
- Ensure your Kafka topic `thomas-cup-matches` has multiple partitions to see consumer group/partition behavior.
- Use the API endpoint `/api/match-results` to send match results as JSON.
- The project enforces badminton match rules and idempotency at the producer level.
