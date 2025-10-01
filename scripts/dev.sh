#!/bin/bash

# Thomas Cup Kafka - Quick Commands Reference
# Save this as an alias or run directly

echo "🏸 Thomas Cup Kafka - Quick Commands"
echo "=================================="

case "${1:-help}" in
    "start"|"up")
        echo "🚀 Starting full stack..."
        docker compose up -d
        echo "⏳ Waiting for services to be ready..."
        sleep 15
        ./setup-kafka-topics-with-replicas.sh
        echo "✅ Infrastructure ready! Now run: mvn spring-boot:run"
        ;;
    
    "stop"|"down")
        echo "🛑 Stopping all services..."
        docker compose down
        echo "✅ All services stopped"
        ;;
    
    "restart")
        echo "🔄 Restarting services..."
        docker compose down
        docker compose up -d
        sleep 15
        ./setup-kafka-topics-with-replicas.sh
        echo "✅ Services restarted"
        ;;
    
    "build")
        echo "🔨 Building application..."
        mvn clean install
        echo "✅ Build complete"
        ;;
    
    "run")
        echo "🏃 Starting Spring Boot application..."
        mvn spring-boot:run
        ;;
    
    "test")
        echo "🧪 Running all tests..."
        mvn test
        ;;
    
    "test-api")
        echo "🌐 Testing API endpoint..."
        curl -X POST http://localhost:8080/api/match-results \
          -H "Content-Type: application/json" \
          -d '{
            "id": "test-'$(date +%s)'",
            "teamA": "Malaysia",
            "teamB": "Indonesia",
            "teamAScore": 21,
            "teamBScore": 19,
            "winner": "Malaysia",
            "matchDateTime": "'$(date -Iseconds | cut -d'+' -f1)'",
            "gameNumber": 1
          }'
        ;;
    
    "k6")
        echo "⚡ Running k6 load test..."
        ../k6/run-tests.sh load
        ;;
    
    "logs")
        echo "📋 Showing recent application logs..."
        docker compose logs --tail=50 -f kafka postgres
        ;;
    
    "status")
        echo "📊 Checking system status..."
        echo "=== Docker Services ==="
        docker compose ps
        echo
        echo "=== Application Health ==="
        curl -s http://localhost:8080/actuator/health | jq 2>/dev/null || curl -s http://localhost:8080/actuator/health
        echo
        echo "=== Kafka Topics ==="
        docker exec kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null || echo "Kafka not accessible"
        ;;
    
    "clean")
        echo "🧹 Deep clean - removing all containers and volumes..."
        read -p "This will delete all data. Continue? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker compose down -v
            docker system prune -f
            mvn clean
            echo "✅ Clean complete"
        else
            echo "❌ Clean cancelled"
        fi
        ;;
    
    "help"|*)
        echo "Usage: $0 <command>"
        echo
        echo "Available commands:"
        echo "  start     - Start all Docker services and setup Kafka topics"
        echo "  stop      - Stop all Docker services"
        echo "  restart   - Restart all services"
        echo "  build     - Build the Maven project"
        echo "  run       - Start the Spring Boot application"
        echo "  test      - Run all unit and integration tests"
        echo "  test-api  - Send a test request to the API"
        echo "  k6        - Run k6 load tests"
        echo "  logs      - Show live logs from Kafka and PostgreSQL"
        echo "  status    - Show status of all services"
        echo "  clean     - Deep clean (removes all data)"
        echo
        echo "Typical workflow:"
        echo "  $0 start    # Start infrastructure"
        echo "  $0 build    # Build application"
        echo "  $0 run      # Run application (in separate terminal)"
        echo "  $0 test-api # Test the API"
        echo "  $0 k6       # Performance test"
        ;;
esac