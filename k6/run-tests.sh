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

# Check if Docker services are running
check_docker() {
    echo -e "${YELLOW}Checking Docker services...${NC}"
    if docker compose ps postgres | grep -q "running"; then
        echo -e "${GREEN}✓ PostgreSQL is running${NC}"
    else
        echo -e "${RED}✗ PostgreSQL not running${NC}"
        echo "Please start with: docker compose up -d postgres"
        exit 1
    fi
    
    if docker compose ps kafka | grep -q "running"; then
        echo -e "${GREEN}✓ Kafka is running${NC}"
    else
        echo -e "${RED}✗ Kafka not running${NC}"
        echo "Please start with: docker compose up -d kafka"
        exit 1
    fi
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
    echo -e "${GREEN}🔥 Starting Soak Test (40 minutes)${NC}"
    echo -e "${YELLOW}This is a long-running test. Press Ctrl+C to cancel.${NC}"
    read -p "Continue? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker compose --profile performance run --rm k6 run /scripts/soak-test.js
    else
        echo "Soak test cancelled"
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
        "all")
            echo -e "${YELLOW}Running all tests sequentially...${NC}"
            run_load_test
            echo -e "${YELLOW}Waiting 30 seconds before spike test...${NC}"
            sleep 30
            run_spike_test
            echo -e "${YELLOW}Soak test skipped in 'all' mode (too long)${NC}"
            ;;
        *)
            echo "Usage: $0 [load|spike|soak|all]"
            echo "  load  - Standard load test (default)"
            echo "  spike - Traffic spike test" 
            echo "  soak  - 30-minute endurance test"
            echo "  all   - Run load + spike tests"
            exit 1
            ;;
    esac
    
    echo -e "${GREEN}✅ Test completed!${NC}"
    echo "Check results in terminal output above"
    echo "Monitor ongoing effects in Prometheus (http://localhost:9090)"
}

main "$@"