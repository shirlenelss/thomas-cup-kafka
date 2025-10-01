import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter, Trend } from 'k6/metrics';

// Custom metrics for comprehensive badminton testing
const matchResultsSent = new Counter('match_results_sent');
const newGamesSent = new Counter('new_games_sent');
const scoreUpdatesSent = new Counter('score_updates_sent');
const totalKafkaMessages = new Counter('total_kafka_messages');
const kafkaErrors = new Rate('kafka_errors');
const apiResponseTime = new Trend('api_response_time');

export const options = {
  stages: [
    { duration: '30s', target: 6 },    // Ramp up to 6 users over 30 seconds
    { duration: '2m', target: 6 },     // Stay at 6 users for 2 minutes  
    { duration: '30s', target: 0 },    // Ramp down over 30 seconds
  ],
  thresholds: {
    http_req_duration: ['p(50)<300', 'p(90)<600', 'p(95)<1000'],
    http_req_failed: ['rate<0.05'],
    match_results_sent: ['count>30'],
    new_games_sent: ['count>20'], 
    score_updates_sent: ['count>40'],
    total_kafka_messages: ['count>90'],
    kafka_errors: ['rate<0.01'],
    api_response_time: ['p(95)<1200'],
  },
};

// macOS Docker compatibility
const BASE_URL = 'http://host.docker.internal:8080';

// Badminton players and tournaments for realistic data
const players = [
  'Lin Dan', 'Lee Chong Wei', 'Viktor Axelsen', 'Kento Momota',
  'Chen Long', 'Anthony Ginting', 'Anders Antonsen', 'Chou Tien Chen',
  'Shi Yuqi', 'Ng Ka Long', 'Kidambi Srikanth', 'Lakshya Sen'
];

const tournaments = [
  'Thomas Cup 2024', 'BWF World Championships', 'All England Open',
  'Indonesia Masters', 'Malaysia Open', 'China Open', 'Denmark Open'
];

const rounds = ['Group Stage', 'Round of 16', 'Quarter Final', 'Semi Final', 'Final'];

export function setup() {
  console.log('🏸 Starting COMPREHENSIVE Thomas Cup Kafka Load Test for macOS');
  console.log('Testing ALL topics: thomas-cup-matches, new-game, update-score');
  
  // Health check
  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
  if (healthCheck.status !== 200) {
    console.error('❌ API health check failed - ensure Spring Boot app is running');
    return { healthy: false };
  }
  
  console.log('✅ API health check passed - ready for comprehensive testing');
  return { healthy: true };
}

export default function(data) {
  if (!data.healthy) {
    console.error('Skipping test due to failed health check');
    return;
  }

  const matchId = `match-${Date.now()}-${Math.floor(Math.random() * 9999)}`;
  const gameNumber = Math.floor(Math.random() * 3) + 1; // Games 1, 2, or 3
  
  // Generate realistic badminton match data
  const baseMatchData = {
    id: matchId,
    matchId: matchId,
    gameNumber: gameNumber,
    playerAScore: Math.floor(Math.random() * 31), // 0-30 points
    playerBScore: Math.floor(Math.random() * 31),
    tournament: tournaments[Math.floor(Math.random() * tournaments.length)],
    round: rounds[Math.floor(Math.random() * rounds.length)],
    player1: players[Math.floor(Math.random() * players.length)],
    player2: players[Math.floor(Math.random() * players.length)]
  };

  // Randomly choose which endpoint to test (weighted distribution)
  const endpointChoice = Math.random();
  let endpoint, topic, counter, actionDescription;
  
  if (endpointChoice < 0.4) {
    // 40% - Main match results (thomas-cup-matches topic)
    endpoint = '/api/match-results';
    topic = 'thomas-cup-matches';
    counter = matchResultsSent;
    actionDescription = 'Match Result';
  } else if (endpointChoice < 0.65) {
    // 25% - New game starts (new-game topic)
    endpoint = '/api/new-game';
    topic = 'new-game'; 
    counter = newGamesSent;
    actionDescription = 'New Game';
    // Reset scores for new game
    baseMatchData.playerAScore = 0;
    baseMatchData.playerBScore = 0;
  } else {
    // 35% - Score updates (update-score topic)
    endpoint = '/api/update-score';
    topic = 'update-score';
    counter = scoreUpdatesSent;
    actionDescription = 'Score Update';
    // Ensure realistic in-progress scores
    baseMatchData.playerAScore = Math.floor(Math.random() * 25) + 1; // 1-25
    baseMatchData.playerBScore = Math.floor(Math.random() * 25) + 1; // 1-25
  }

  const startTime = Date.now();
  
  const response = http.post(`${BASE_URL}${endpoint}`, JSON.stringify(baseMatchData), {
    headers: {
      'Content-Type': 'application/json',
    },
  });
  
  const endTime = Date.now();
  const responseTime = endTime - startTime;
  
  // Record metrics
  apiResponseTime.add(responseTime);
  counter.add(1);
  totalKafkaMessages.add(1);
  
  // Comprehensive response validation
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'response contains Kafka message': (r) => r.body && r.body.includes('Kafka'),
    'response time < 1000ms': (r) => r.timings.duration < 1000,
    'response time < 1500ms': (r) => r.timings.duration < 1500,
    'has content-type header': (r) => r.headers['Content-Type'] !== undefined,
    'body is not empty': (r) => r.body && r.body.length > 0,
    'no server errors': (r) => r.status < 500,
  });
  
  if (response.status !== 200 || !response.body || !response.body.includes('Kafka')) {
    kafkaErrors.add(1);
    console.error(`❌ Failed ${actionDescription} for ${matchId}: ${response.status} - ${response.body ? response.body.substring(0, 100) : 'null'}`);
  } else {
    console.log(`✅ ${actionDescription} ${matchId} → ${topic} topic (${responseTime}ms)`);
  }
  
  // Simulate realistic user behavior with variable sleep
  sleep(Math.random() * 0.6 + 0.1); // 100-700ms
}

export function teardown(data) {
  if (data.healthy) {
    console.log('🎯 COMPREHENSIVE LOAD TEST COMPLETED!');
    console.log('📊 Check the following for results:');
    console.log('   • Kafka UI: http://localhost:9001 (all topics)');
    console.log('   • Grafana: http://localhost:3001 (metrics dashboard)');  
    console.log('   • Database: PostgreSQL for persisted match data');
    console.log('   • Application logs: Consumer processing across all topics');
    console.log('🏸 Thomas Cup Kafka system fully tested across all message flows!');
  }
}