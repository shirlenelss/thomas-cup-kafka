#!/bin/bash

# Quick Thomas Cup Match Demo - Single Game
# Generates rapid Kafka traffic for dashboard testing

API_URL="http://localhost:8080/api/match-results"
MATCH_ID="demo-$(date +%s)"
CURRENT_DATE=$(date -u +"%Y-%m-%dT%H:%M:%S")

echo "🏸 Quick Demo: Malaysia vs Indonesia (Game 1 to 21)"

score_a=0
score_b=0

# Rapid fire scoring to 21
for i in {1..42}; do
    if [ $((RANDOM % 100)) -lt 60 ]; then
        score_a=$((score_a + 1))
    else
        score_b=$((score_b + 1))
    fi
    
    winner="null"
    if [ $score_a -ge 21 ] && [ $((score_a - score_b)) -ge 2 ]; then
        winner="\"Malaysia\""
    elif [ $score_b -ge 21 ] && [ $((score_b - score_a)) -ge 2 ]; then
        winner="\"Indonesia\""
    fi
    
    echo "📊 $score_a - $score_b"
    
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "{
            \"id\": \"$MATCH_ID\",
            \"teamA\": \"Malaysia\",
            \"teamB\": \"Indonesia\",
            \"teamAScore\": $score_a,
            \"teamBScore\": $score_b,
            \"winner\": $winner,
            \"matchDateTime\": \"$CURRENT_DATE\",
            \"gameNumber\": 1
        }" > /dev/null
    
    sleep 0.2
    
    if [[ $winner != "null" ]]; then
        echo "🏆 Game complete!"
        break
    fi
done

echo "✅ Demo complete! Check your Grafana dashboard."