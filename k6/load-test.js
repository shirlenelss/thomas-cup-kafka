import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const badmintonMatchesCounter = new Counter('badminton_matches_sent');
const kafkaErrorRate = new Rate('kafka_errors');
const matchProcessingTime = new Trend('match_processing_duration');

// Base URL (configurable). When running k6 in Docker on macOS/Windows, use host.docker.internal
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
// Simulation mode to print payloads instead of sending HTTP
const SIMULATE = (__ENV.SIMULATE === '1') || (__ENV.DRY_RUN === '1');

// Test configuration - Enhanced load test with realistic scaling
export const options = {
  stages: [
    { duration: '1m', target: 10 },   // Warm up
    { duration: '2m', target: 25 },   // Normal load
    { duration: '2m', target: 50 },   // Increased load
    { duration: '3m', target: 75 },   // Peak load
    { duration: '2m', target: 100 },  // Maximum sustainable load
    { duration: '1m', target: 50 },   // Scale back
    { duration: '1m', target: 0 },    // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(50)<200', 'p(90)<500', 'p(95)<800'], // Response time percentiles
    http_req_failed: ['rate<0.05'],   // Error rate under 5%
    kafka_errors: ['rate<0.01'],      // Kafka error rate under 1%
    badminton_matches_sent: ['count>500'], // Minimum matches processed
    match_processing_duration: ['p(95)<1000'], // End-to-end processing time
  },
  // Resource monitoring
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],
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

// Helper: create a new-game payload from a final result (same id/teams/gameNumber, zeroed scores, no winner)
function deriveNewGame(final) {
  return {
    id: final.id,
    teamA: final.teamA,
    teamB: final.teamB,
    teamAScore: 0,
    teamBScore: 0,
    winner: '',
    matchDateTime: final.matchDateTime,
    gameNumber: final.gameNumber,
  };
}

// Generate a valid badminton game score sequence under official rules
function simulateGameSequence(gameNumber) {
  const maxPoints = gameNumber === 3 ? 15 : 21;
  const cap = 30;
  let a = 0, b = 0;
  const sequence = [{ a, b }]; // include starting 0-0 for clarity

  // Random bias per game to avoid symmetric flip-flop
  const bias = 0.5 + (Math.random() - 0.5) * 0.2; // 0.4..0.6

  while (true) {
    // Choose rally winner
    if (Math.random() < bias) a++; else b++;

    // Push the new score snapshot
    sequence.push({ a, b });

    // Check terminal conditions
    const reachedBase = (a >= maxPoints || b >= maxPoints) && Math.abs(a - b) >= 2;
    const reachedCap = a === cap || b === cap;
    if (reachedBase || reachedCap) {
      return sequence;
    }
  }
}

function pickTeams() {
  const teams = ['TeamA', 'TeamB', 'TeamC', 'TeamD', 'Malaysia', 'Indonesia', 'China', 'Japan'];
  const teamA = teams[Math.floor(Math.random() * teams.length)];
  let teamB = teams[Math.floor(Math.random() * teams.length)];
  while (teamB === teamA) teamB = teams[Math.floor(Math.random() * teams.length)];
  return { teamA, teamB };
}

function buildMatchScenario() {
  const { teamA, teamB } = pickTeams();
  const gameNumber = Math.floor(Math.random() * 3) + 1; // 1..3
  const sequence = simulateGameSequence(gameNumber); // [{a,b}, ...]
  const last = sequence[sequence.length - 1];
  const winner = last.a > last.b ? teamA : teamB;
  const id = `match-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  const matchDateTime = new Date().toISOString().slice(0, -1);
  return { id, teamA, teamB, gameNumber, sequence, winner, matchDateTime };
}

export default function () {
  const sim = buildMatchScenario();

  // new-game payload (always 0-0)
  const newGame = {
    id: sim.id,
    teamA: sim.teamA,
    teamB: sim.teamB,
    teamAScore: 0,
    teamBScore: 0,
    winner: '',
    matchDateTime: sim.matchDateTime,
    gameNumber: sim.gameNumber,
  };

  // final payload (terminal score)
  const last = sim.sequence[sim.sequence.length - 1];
  const finalResult = {
    id: sim.id,
    teamA: sim.teamA,
    teamB: sim.teamB,
    teamAScore: last.a,
    teamBScore: last.b,
    winner: sim.winner,
    matchDateTime: sim.matchDateTime,
    gameNumber: sim.gameNumber,
  };

  if (SIMULATE) {
    console.log('SIMULATED new-game payload:', JSON.stringify(newGame));
    console.log(`SIMULATED update-score sequence (${sim.sequence.length - 1} events):`);
    // Skip the first 0-0 snapshot; send one event per point thereafter
    for (let i = 1; i < sim.sequence.length; i++) {
      const step = sim.sequence[i];
      const isLast = i === sim.sequence.length - 1;
      const payload = {
        id: sim.id,
        teamA: sim.teamA,
        teamB: sim.teamB,
        teamAScore: step.a,
        teamBScore: step.b,
        winner: isLast ? sim.winner : '',
        matchDateTime: sim.matchDateTime,
        gameNumber: sim.gameNumber,
      };
      console.log('-', JSON.stringify(payload));
    }
    sleep(0.1);
    return;
  }

  const params = { headers: { 'Content-Type': 'application/json' } };

  const startTime = Date.now();
  const resNew = http.post(`${BASE_URL}/api/new-game`, JSON.stringify(newGame), params);

  let ok = check(resNew, { 'new-game status is 200': (r) => r.status === 200 });
  if (!ok) {
    kafkaErrorRate.add(1);
    console.error(`new-game failed for ${sim.id}: ${resNew.status} - ${resNew.body}`);
    return; // abort this sequence
  }

  // Post each incremental score as an update-score event
  for (let i = 1; i < sim.sequence.length; i++) {
    const step = sim.sequence[i];
    const isLast = i === sim.sequence.length - 1;
    const payload = {
      id: sim.id,
      teamA: sim.teamA,
      teamB: sim.teamB,
      teamAScore: step.a,
      teamBScore: step.b,
      winner: isLast ? sim.winner : '',
      matchDateTime: sim.matchDateTime,
      gameNumber: sim.gameNumber,
    };
    const resUpd = http.post(`${BASE_URL}/api/update-score`, JSON.stringify(payload), params);

    ok = check(resUpd, { 'update-score status is 200/201': (r) => r.status === 200 || r.status === 201 });
    if (!ok) {
      kafkaErrorRate.add(1);
      console.error(`update-score failed for ${sim.id} at ${step.a}-${step.b}: ${resUpd.status} - ${resUpd.body}`);
      break;
    }

    // small think-time between points
    sleep(0.05 + Math.random() * 0.1);
  }

  const endTime = Date.now();
  badmintonMatchesCounter.add(1);
  matchProcessingTime.add(endTime - startTime);
}

// Setup function - runs once before the test starts
export function setup() {
  console.log('Starting badminton match load test...');
  console.log('Testing Thomas Cup Kafka API with realistic match scenarios');

  if (SIMULATE) {
    console.log('SIMULATE=1 set: will only print payloads, no HTTP calls');
    return { startTime: Date.now() };
  }

  const healthCheck = http.get(`${BASE_URL}/actuator/health`);
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