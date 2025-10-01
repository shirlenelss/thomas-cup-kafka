# Scripts Directory

This directory contains all shell scripts for the Thomas Cup Kafka project automation and testing.

## 🚀 Quick Start Script

### `dev.sh` - Development Workflow Manager
**Primary script for all development tasks**

```bash
./scripts/dev.sh <command>
```

#### Available Commands:
- `start` - Start all Docker services and setup Kafka topics
- `stop` - Stop all Docker services  
- `restart` - Restart all services
- `build` - Build the Maven project
- `run` - Start the Spring Boot application
- `test` - Run all unit and integration tests
- `test-api` - Send a test request to the API
- `k6` - Run k6 load tests
- `logs` - Show live logs from Kafka and PostgreSQL
- `status` - Show status of all services
- `clean` - Deep clean (removes all data)

#### Typical Workflow:
```bash
./scripts/dev.sh start     # Start infrastructure
./scripts/dev.sh build     # Build application  
./scripts/dev.sh run       # Run application (in separate terminal)
./scripts/dev.sh test-api  # Test the API
./scripts/dev.sh k6        # Performance test
```

## 🔧 Infrastructure Scripts

### `setup-kafka-topics-with-replicas.sh`
Sets up Kafka topics with 3-broker replication for production readiness.

**Topics Created:**
- `thomas-cup-matches` (3 partitions, replication factor 3)
- `new-game` (3 partitions, replication factor 3) 
- `update-score` (3 partitions, replication factor 3)

**Usage:**
```bash
./scripts/setup-kafka-topics-with-replicas.sh
```

**Prerequisites:** 3-broker Kafka cluster must be running

## 🎮 Demo & Testing Scripts

### `full-system-demo.sh`
Complete end-to-end demonstration of the system functionality.

**What it does:**
- Starts all services
- Sets up Kafka topics with replication
- Builds and runs the application
- Sends sample match data
- Shows system status and logs

**Usage:**
```bash
./scripts/full-system-demo.sh
```

### `simulate-badminton-match.sh`
Simulates realistic badminton match data for testing.

**Features:**
- Creates realistic match scenarios
- Sends data to multiple Kafka topics
- Includes proper badminton scoring (21/21/15 point system)
- Tests idempotency with duplicate prevention

**Usage:**
```bash
./scripts/simulate-badminton-match.sh
```

## 📊 Configuration Files

### `badminton-championship-dashboard.json`
Grafana dashboard configuration for monitoring badminton match processing.

**Features:**
- Kafka message throughput metrics
- Database insertion rates
- Match processing latency
- System health indicators

**Usage:** Import into Grafana for monitoring

## 🏗️ Architecture Context

### Multi-Broker Kafka Setup
All scripts are designed for the 3-broker Kafka cluster:
- **kafka1:9092** - Primary broker
- **kafka2:9093** - Secondary broker  
- **kafka3:9094** - Tertiary broker
- **Replication Factor:** 3 for fault tolerance
- **Min ISR:** 2 for consistency

### Topic Strategy
- **thomas-cup-matches**: Main event stream
- **new-game**: Database inserts with conflict handling
- **update-score**: Database updates for existing games

## 🔍 Troubleshooting

### Common Issues:

1. **"Topics already exist" error**
   ```bash
   # Normal behavior - topics are idempotent
   ./scripts/setup-kafka-topics-with-replicas.sh
   ```

2. **Kafka not ready**
   ```bash
   # Wait for all brokers to start
   docker compose logs kafka1 kafka2 kafka3
   ```

3. **Permission denied**
   ```bash
   chmod +x scripts/*.sh
   ```

## 📁 Related Directories

- `../k6/` - Performance testing scripts (k6 load tests)
- `../docker/` - Database setup scripts for external PostgreSQL deployments
  - `init-db.sh` - External PostgreSQL database setup
  - `setup-external-db-example.sh` - Usage examples for cloud databases
- `../src/test/` - Java integration and unit tests

## 🔗 Quick Links

- [Main README](../README.md) - Project overview
- [Architecture Plan](../ARCHITECTURE_MIGRATION_PLAN.md) - Technical details
- [K6 Performance Tests](../k6/README.md) - Load testing guide