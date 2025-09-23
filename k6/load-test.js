import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const badmintonMatchesCounter = new Counter('badminton_matches_sent');
const kafkaErrorRate = new Rate('kafka_errors');
const matchProcessingTime = new Trend('match_processing_duration');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 20 }, // Ramp up to 20 users
    { duration: '1m', target: 20 },  // Stay at 20 users
    { duration: '30s', target: 50 }, // Ramp up to 50 users
    { duration: '2m', target: 50 },  // Stay at 50 users
    { duration: '30s', target: 0 },  // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests under 500ms
    http_req_failed: ['rate<0.05'],   // Error rate under 5%
    kafka_errors: ['rate<0.01'],      // Kafka error rate under 1%
  },
};

// Generate realistic badminton match data
function generateMatchResult() {
  const teams = ['TeamA', 'TeamB', 'TeamC', 'TeamD', 'Malaysia', 'Indonesia', 'China', 'Japan'];
  const teamA = teams[Math.floor(Math.random() * teams.length)];
  let teamB = teams[Math.floor(Math.random() * teams.length)];
  while (teamB === teamA) {
    teamB = teams[Math.floor(Math.random() * teams.length)];
  }
  
  const gameNumber = Math.floor(Math.random() * 3) + 1; // 1, 2, or 3
  const maxPoints = gameNumber === 3 ? 15 : 21;
  
  // Generate realistic badminton scores
  let teamAScore, teamBScore, winner;
  const isCloseGame = Math.random() > 0.3; // 70% chance of close game
  
  if (isCloseGame) {
    // Close game - could go to deuce (up to 30 points)
    const baseScore = Math.floor(Math.random() * 5) + maxPoints - 2; // Near the limit
    teamAScore = baseScore + Math.floor(Math.random() * 3);
    teamBScore = baseScore + Math.floor(Math.random() * 3);
    
    // Ensure one team wins by at least 2 points or cap at 30
    if (Math.abs(teamAScore - teamBScore) < 2 && Math.max(teamAScore, teamBScore) < 30) {
      teamAScore = Math.min(teamAScore + 2, 30);
    }
  } else {
    // Decisive win
    const winningScore = Math.floor(Math.random() * 5) + maxPoints;
    const losingScore = Math.floor(Math.random() * (maxPoints - 5));
    if (Math.random() > 0.5) {
      teamAScore = winningScore;
      teamBScore = losingScore;
    } else {
      teamAScore = losingScore;
      teamBScore = winningScore;
    }
  }
  
  winner = teamAScore > teamBScore ? teamA : teamB;
  
  return {
    id: `match-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
    teamA,
    teamB,
    teamAScore,
    teamBScore,
    winner,
    matchDateTime: new Date().toISOString().slice(0, -1), // Remove 'Z' for LocalDateTime format
    gameNumber
  };
}

export default function () {
  const matchResult = generateMatchResult();
  
  const payload = JSON.stringify(matchResult);
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  const startTime = Date.now();
  const response = http.post('http://host.docker.internal:8080/api/match-results', payload, params);
  const endTime = Date.now();
  
  // Record custom metrics
  badmintonMatchesCounter.add(1);
  matchProcessingTime.add(endTime - startTime);
  
  // Check response
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response contains success message': (r) => r.body.includes('Match result sent to Kafka'),
    'response time < 1000ms': (r) => r.timings.duration < 1000,
  });
  
  if (!success) {
    kafkaErrorRate.add(1);
    console.error(`Failed request for match ${matchResult.id}: ${response.status} - ${response.body}`);
  } else {
    kafkaErrorRate.add(0);
  }
  
  // Realistic think time between requests
  sleep(Math.random() * 2 + 0.5); // 0.5-2.5 seconds
}

// Setup function - runs once before the test starts
export function setup() {
  console.log('Starting badminton match load test...');
  console.log('Testing Thomas Cup Kafka API with realistic match scenarios');
  
  // Verify API is accessible
  const healthCheck = http.get('http://host.docker.internal:8080/actuator/health');
  if (healthCheck.status !== 200) {
    console.error('API health check failed - ensure Spring Boot app is running');
  }
  
  return { startTime: Date.now() };
}

// Teardown function - runs once after the test ends
export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  console.log(`Load test completed in ${duration} seconds`);
  console.log('Check Kafka topics and database for processed match results');
}