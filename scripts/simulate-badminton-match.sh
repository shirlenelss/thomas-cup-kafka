#!/bin/bash

# Thomas Cup Badminton Match Simulator
# Simulates a realistic badminton match with point-by-point updates
# Games 1&2 play to 21 points, Game 3 to 15 points (all capped at 30)

set -e

API_URL="http://localhost:8080/api/match-results"
MATCH_ID="thomas-cup-$(date +%s)"
TEAM_A="Malaysia"
TEAM_B="Indonesia" 
CURRENT_DATE=$(date -u +"%Y-%m-%dT%H:%M:%S")

echo "🏸 Starting Thomas Cup Match Simulation: $TEAM_A vs $TEAM_B"
echo "📊 Match ID: $MATCH_ID"
echo "🕒 Match Time: $CURRENT_DATE"
echo "=================================="

# Function to send match update
send_update() {
    local game_num=$1
    local score_a=$2
    local score_b=$3
    local winner=$4
    
    local json_payload=$(cat <<EOF
{
  "id": "$MATCH_ID",
  "teamA": "$TEAM_A",
  "teamB": "$TEAM_B",
  "teamAScore": $score_a,
  "teamBScore": $score_b,
  "winner": $winner,
  "matchDateTime": "$CURRENT_DATE",
  "gameNumber": $game_num
}
EOF
)
    
    echo "🏸 Game $game_num: $TEAM_A $score_a - $score_b $TEAM_B"
    
    curl -s -X POST "$API_URL" \
        -H "Content-Type: application/json" \
        -d "$json_payload" > /dev/null
        
    sleep 0.5  # Small delay between points
}

# Function to simulate a single game
simulate_game() {
    local game_number=$1
    local target_score=21
    
    # Game 3 plays to 15 points
    if [ $game_number -eq 3 ]; then
        target_score=15
    fi
    
    echo ""
    echo "🎯 Starting Game $game_number (first to $target_score points)"
    echo "----------------------------------------"
    
    local score_a=0
    local score_b=0
    local game_winner="null"
    
    while [ $score_a -lt 30 ] && [ $score_b -lt 30 ]; do
        # Simulate realistic scoring - slightly favor one team randomly
        if [ $((RANDOM % 100)) -lt 55 ]; then
            score_a=$((score_a + 1))
        else
            score_b=$((score_b + 1))
        fi
        
        # Check win conditions
        if [ $score_a -ge $target_score ] && [ $((score_a - score_b)) -ge 2 ]; then
            game_winner="\"$TEAM_A\""
            send_update $game_number $score_a $score_b $game_winner
            echo "🏆 $TEAM_A wins Game $game_number!"
            break
        elif [ $score_b -ge $target_score ] && [ $((score_b - score_a)) -ge 2 ]; then
            game_winner="\"$TEAM_B\""
            send_update $game_number $score_a $score_b $game_winner
            echo "🏆 $TEAM_B wins Game $game_number!"
            break
        elif [ $score_a -eq 30 ]; then
            game_winner="\"$TEAM_A\""
            send_update $game_number $score_a $score_b $game_winner
            echo "🏆 $TEAM_A wins Game $game_number (30 point cap)!"
            break
        elif [ $score_b -eq 30 ]; then
            game_winner="\"$TEAM_B\""
            send_update $game_number $score_a $score_b $game_winner
            echo "🏆 $TEAM_B wins Game $game_number (30 point cap)!"
            break
        else
            # Send intermediate score update
            send_update $game_number $score_a $score_b "null"
        fi
        
        # Add some excitement for close games
        if [ $score_a -ge $((target_score - 3)) ] || [ $score_b -ge $((target_score - 3)) ]; then
            sleep 1  # Slower updates for tension
        fi
    done
    
    echo "📊 Final Score Game $game_number: $TEAM_A $score_a - $score_b $TEAM_B"
    
    # Return winner (A or B)
    if [[ $game_winner == *"$TEAM_A"* ]]; then
        return 1  # Team A wins
    else
        return 2  # Team B wins
    fi
}

# Main match simulation
main() {
    echo "🚀 Match simulation starting in 3 seconds..."
    sleep 3
    
    local team_a_games=0
    local team_b_games=0
    local game_number=1
    
    # Play until best of 3 is decided (first to win 2 games)
    while [ $team_a_games -lt 2 ] && [ $team_b_games -lt 2 ] && [ $game_number -le 3 ]; do
        simulate_game $game_number
        local game_result=$?
        
        if [ $game_result -eq 1 ]; then
            team_a_games=$((team_a_games + 1))
            echo "✅ $TEAM_A leads $team_a_games-$team_b_games in games"
        else
            team_b_games=$((team_b_games + 1))
            echo "✅ $TEAM_B leads $team_b_games-$team_a_games in games"
        fi
        
        game_number=$((game_number + 1))
        
        if [ $game_number -le 3 ]; then
            echo ""
            echo "⏸️  30 second break between games..."
            sleep 2  # Shortened for demo
        fi
    done
    
    # Announce match winner
    echo ""
    echo "🎉 MATCH COMPLETE! 🎉"
    if [ $team_a_games -eq 2 ]; then
        echo "🏆 $TEAM_A wins the match $team_a_games-$team_b_games!"
    else
        echo "🏆 $TEAM_B wins the match $team_b_games-$team_a_games!"
    fi
    
    echo ""
    echo "📈 Check your Grafana dashboard for metrics!"
    echo "🔍 Prometheus metrics: http://localhost:8080/actuator/prometheus"
    echo "📊 Grafana dashboard: http://localhost:3001"
}

# Check if Spring Boot is running
echo "🔍 Checking if Thomas Cup API is running..."
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "❌ Error: Spring Boot application is not running on port 8080"
    echo "💡 Please start your application first: mvn spring-boot:run"
    exit 1
fi

echo "✅ API is running, starting match simulation!"

# Run the simulation
main

echo ""
echo "🏸 Thomas Cup Match Simulation Complete!"
echo "📊 Total Kafka messages sent: Multiple point updates per game"
echo "🎯 Your dashboard should now show increased activity!"