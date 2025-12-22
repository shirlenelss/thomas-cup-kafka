#!/bin/bash

# K6 Test Runner for Thomas Cup Kafka
# Usage: ./run-tests.sh [load|spike|soak|all]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'  
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Spring Boot app is running
check_app() {
    echo -e "${YELLOW}Checking if Spring Boot application is running...${NC}"
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        echo -e "${GREEN}✓ Application is running${NC}"
    else
        echo -e "${RED}✗ Application not accessible at http://localhost:8080${NC}"
        echo "Please start the application with: mvn spring-boot:run"
        exit 1
    fi
}

# Check if Docker services are running (robust for Compose V2)
check_docker() {
    echo -e "${YELLOW}Checking Docker services...${NC}"

    # Postgres: ensure service is running and (if present) healthy
    if docker compose ps --status running --services | grep -qx "postgres"; then
        CID=$(docker compose ps -q postgres)
        # Health may be undefined; treat 'none' as OK, but prefer 'healthy'
        HEALTH=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$CID" 2>/dev/null || echo none)
        if [ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "none" ]; then
            echo -e "${GREEN}✓ PostgreSQL is running (${HEALTH})${NC}"
        else
            echo -e "${RED}✗ PostgreSQL container is not healthy (status=$HEALTH)${NC}"
            echo "Please check: docker compose logs -f postgres"
            exit 1
        fi
    else
        echo -e "${RED}✗ PostgreSQL not running${NC}"
        echo "Please start with: docker compose up -d postgres"
        exit 1
    fi


    # Kafka: this stack uses kafka1/kafka2/kafka3 services
    local brokers=("kafka1" "kafka2" "kafka3")
    for b in "${brokers[@]}"; do
        if ! docker compose ps --status running --services | grep -qx "$b"; then
            echo -e "${RED}✗ $b not running${NC}"
            echo "Start with: docker compose up -d $b"
            exit 1
        fi
    done
    echo -e "${GREEN}✓ Kafka brokers are running (kafka1, kafka2, kafka3)${NC}"
}

# Run load test
run_load_test() {
    echo -e "${GREEN}🚀 Starting Load Test (4 minutes)${NC}"
    docker compose --profile performance run --rm k6 run /scripts/load-test.js
}

# Run spike test  
run_spike_test() {
    echo -e "${GREEN}⚡ Starting Spike Test (1 minute)${NC}"
    docker compose --profile performance run --rm k6 run /scripts/spike-test.js
}

# Run soak test
run_soak_test() {
    echo -e "${GREEN}🔥 Starting Soak Test (60 minutes)${NC}"
    echo -e "${YELLOW}This is a long-running test. Press Ctrl+C to cancel.${NC}"
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker compose --profile performance run --rm k6 run /scripts/soak-test.js
    else
        echo "Soak test cancelled"
    fi
}

# Run comprehensive test
run_comprehensive_test() {
    echo -e "${GREEN}🎯 Starting Comprehensive Performance Test (26 minutes)${NC}"
    echo -e "${YELLOW}This combines load, spike, and endurance testing.${NC}"
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker compose --profile performance run --rm k6 run /scripts/comprehensive-test.js
    else
        echo "Comprehensive test cancelled"
    fi
}

# Main execution
main() {
    echo -e "${GREEN}Thomas Cup Kafka - K6 Performance Tests${NC}"
    echo "================================================"
    
    check_app
    check_docker
    
    case "${1:-load}" in
        "load")
            run_load_test
            ;;
        "spike") 
            run_spike_test
            ;;
        "soak")
            run_soak_test
            ;;
        "comprehensive"|"comp")
            run_comprehensive_test
            ;;
        "all")
            echo -e "${YELLOW}Running all tests sequentially (except soak)...${NC}"
            run_load_test
            echo -e "${YELLOW}Waiting 30 seconds before spike test...${NC}"
            sleep 30
            run_spike_test
            echo -e "${YELLOW}Waiting 60 seconds before comprehensive test...${NC}"
            sleep 60
            run_comprehensive_test
            ;;
        *)
            echo "Usage: $0 [load|spike|soak|comprehensive|all]"
            echo "  load         - Enhanced load test (12 minutes)"
            echo "  spike        - Multi-phase spike test (4 minutes)" 
            echo "  soak         - Extended endurance test (60 minutes)"
            echo "  comprehensive- Complete performance test (26 minutes)"
            echo "  all          - Run load + spike + comprehensive (sequential)"
            exit 1
            ;;
    esac
    
    echo -e "${GREEN}✅ Test completed!${NC}"
    echo "Check results in terminal output above"
    echo "Monitor ongoing effects in Prometheus (http://localhost:9090)"
}

main "$@"