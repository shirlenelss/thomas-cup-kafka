import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Custom metrics for badminton match testing
const badmintonMatchesSent = new Counter('badminton_matches_sent');
const kafkaErrors = new Rate('kafka_errors');
const matchProcessingDuration = new Trend('match_processing_duration');

export const options = {
  stages: [
    { duration: '30s', target: 5 },    // Ramp up to 5 users over 30 seconds
    { duration: '1m', target: 5 },     // Stay at 5 users for 1 minute
    { duration: '30s', target: 0 },    // Ramp down over 30 seconds
  ],
  thresholds: {
    http_req_duration: ['p(50)<200', 'p(90)<500', 'p(95)<800'],
    http_req_failed: ['rate<0.05'],
    badminton_matches_sent: ['count>50'],
    kafka_errors: ['rate<0.01'],
    match_processing_duration: ['p(95)<1000'],
  },
};

// Updated base URL for macOS Docker compatibility
const BASE_URL = 'http://host.docker.internal:8080';

export function setup() {
  console.log('Starting badminton match load test for macOS...');
  console.log('Testing Thomas Cup Kafka API with realistic match scenarios');
  
  // Check if the API is available
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  if (healthCheck.status !== 200) {
    console.error('API health check failed - ensure Spring Boot app is running');
  }
}

export default function() {
  const matchId = `match-${Date.now()}-${Math.floor(Math.random() * 9999)}`;
  
  // Simulate realistic badminton match data
  const matchData = {
    id: matchId,
    matchId: matchId,
    gameNumber: Math.floor(Math.random() * 3) + 1, // Games 1, 2, or 3
    playerAScore: Math.floor(Math.random() * 31), // 0-30 points (badminton max)
    playerBScore: Math.floor(Math.random() * 31),
    tournament: 'Thomas Cup 2024',
    round: ['Group Stage', 'Quarter Final', 'Semi Final', 'Final'][Math.floor(Math.random() * 4)],
    player1: ['Lin Dan', 'Lee Chong Wei', 'Viktor Axelsen', 'Kento Momota'][Math.floor(Math.random() * 4)],
    player2: ['Chen Long', 'Anthony Ginting', 'Anders Antonsen', 'Chou Tien Chen'][Math.floor(Math.random() * 4)]
  };

  const startTime = Date.now();
  
  const response = http.post(`${BASE_URL}/api/match-results`, JSON.stringify(matchData), {
    headers: {
      'Content-Type': 'application/json',
    },
  });
  
  const endTime = Date.now();
  const processingTime = endTime - startTime;
  
  // Record processing time
  matchProcessingDuration.add(processingTime);
  
  // Increment matches sent counter
  badmintonMatchesSent.add(1);
  
  // Check response and record errors
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response contains Kafka message': (r) => r.body && r.body.includes('Kafka'),
    'response time < 500ms': (r) => r.timings.duration < 500,
    'response time < 1000ms': (r) => r.timings.duration < 1000,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
    'has content-type header': (r) => r.headers['Content-Type'] !== undefined,
    'body is not empty': (r) => r.body && r.body.length > 0,
  });
  
  if (response.status !== 200 || !response.body || !response.body.includes('Kafka')) {
    kafkaErrors.add(1);
    console.error(`Failed request for match ${matchId}: ${response.status} - ${response.body ? response.body.substring(0, 100) : 'null'}`);
  } else {
    console.log(`✅ Match ${matchId} successfully sent to Kafka`);
  }
  
  // Sleep for 100-500ms to simulate realistic user behavior
  sleep(Math.random() * 0.4 + 0.1);
}

export function teardown() {
  console.log('Load test completed in', __ENV.TEST_DURATION || 'unknown', 'seconds');
  console.log('Check Kafka topics and database for processed match results');
}