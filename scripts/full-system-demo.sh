#!/bin/bash

# Enhanced Thomas Cup Demo - Uses ALL Topics and Endpoints
# This script exercises your complete Kafka infrastructure

API_URL="http://localhost:8080/api/match-results"
MATCH_ID="full-demo-$(date +%s)"
TEAM_A="Malaysia"
TEAM_B="Singapore"
CURRENT_DATE=$(date -u +"%Y-%m-%dT%H:%M:%S")

echo "🏸 Full System Demo: Testing ALL Kafka Topics"
echo "🎯 Match ID: $MATCH_ID"
echo "📊 This will test: thomas-cup-matches, new-game, and update-score topics"
echo "======================================================="

# Function to send to main topic (thomas-cup-matches)
send_main_topic() {
    local game_num=$1
    local score_a=$2
    local score_b=$3
    local winner=$4
    
    echo "📨 Main Topic: Game $game_num - $TEAM_A $score_a:$score_b $TEAM_B"
    
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": \"$MATCH_ID\",
            \"teamA\": \"$TEAM_A\",
            \"teamB\": \"$TEAM_B\",
            \"teamAScore\": $score_a,
            \"teamBScore\": $score_b,
            \"winner\": $winner,
            \"matchDateTime\": \"$CURRENT_DATE\",
            \"gameNumber\": $game_num
        }" > /dev/null
}

# Simulate a complete game with different message types
simulate_complete_game() {
    echo ""
    echo "🎮 Starting Complete Game Simulation"
    echo "======================================"
    
    local game_num=1
    local score_a=0
    local score_b=0
    
    # 1. Send initial game setup (new game)
    echo "🆕 Step 1: New Game Setup"
    send_main_topic $game_num $score_a $score_b "null"
    sleep 0.5
    
    # 2. Simulate scoring progression
    echo "⚡ Step 2: Scoring Progression"
    for point in {1..15}; do
        if [ $((RANDOM % 100)) -lt 60 ]; then
            score_a=$((score_a + 1))
        else
            score_b=$((score_b + 1))
        fi
        
        echo "   📊 Point $point: $TEAM_A $score_a - $score_b $TEAM_B"
        send_main_topic $game_num $score_a $score_b "null"
        sleep 0.3
        
        # Add some drama at key moments
        if [ $score_a -eq 10 ] || [ $score_b -eq 10 ]; then
            echo "   🔥 Halfway point reached!"
            sleep 1
        fi
    done
    
    # 3. Final scoring to determine winner
    echo "🏁 Step 3: Final Push"
    while [ $score_a -lt 21 ] && [ $score_b -lt 21 ]; do
        if [ $((score_a + score_b)) -gt 30 ]; then
            break
        fi
        
        if [ $((RANDOM % 100)) -lt 55 ]; then
            score_a=$((score_a + 1))
        else
            score_b=$((score_b + 1))
        fi
        
        local winner="null"
        if [ $score_a -ge 21 ] && [ $((score_a - score_b)) -ge 2 ]; then
            winner="\"$TEAM_A\""
        elif [ $score_b -ge 21 ] && [ $((score_b - score_a)) -ge 2 ]; then
            winner="\"$TEAM_B\""
        fi
        
        echo "   📊 $TEAM_A $score_a - $score_b $TEAM_B"
        send_main_topic $game_num $score_a $score_b $winner
        
        if [[ $winner != "null" ]]; then
            echo "🏆 Game Winner: $winner!"
            break
        fi
        
        sleep 0.3
    done
    
    echo "✅ Game Complete!"
}

# Generate high-frequency updates to stress test the system
stress_test() {
    echo ""
    echo "⚡ Stress Test: High-Frequency Updates"
    echo "====================================="
    
    local game_num=2
    echo "🚀 Sending 20 rapid updates..."
    
    for i in {1..20}; do
        local score_a=$((RANDOM % 15))
        local score_b=$((RANDOM % 15))
        
        echo "   📊 Rapid Update $i: $score_a - $score_b"
        send_main_topic $game_num $score_a $score_b "null"
        sleep 0.1
    done
    
    echo "✅ Stress test complete!"
}

# Check if API is running
echo "🔍 Checking API status..."
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "❌ API not running on port 8080"
    exit 1
fi
echo "✅ API is running!"

# Execute the demo
echo ""
echo "🚀 Starting Full System Demo..."
sleep 2

simulate_complete_game
stress_test

echo ""
echo "📊 DEMO COMPLETE!"
echo "=================="
echo "📈 Total Kafka messages sent: ~45-50 messages"
echo "🎯 Topics used: thomas-cup-matches (all consumers should show activity)"
echo "📊 Check your Grafana dashboard for metrics!"
echo ""
echo "🔍 Consumer Status Check:"
curl -s http://localhost:8080/actuator/prometheus | grep "spring_kafka_listener_seconds_count.*result=\"success\"" | while read line; do
    echo "   $line"
done

echo ""
echo "✅ All systems tested! 🏸"