import http from 'k6/http';
import { check, sleep } from 'k6';

// Spike test configuration - sudden traffic bursts
export const options = {
  stages: [
    { duration: '10s', target: 1 },   // Normal load
    { duration: '10s', target: 100 }, // Spike to 100 users
    { duration: '30s', target: 100 }, // Stay at spike level
    { duration: '10s', target: 1 },   // Drop back to normal
    { duration: '10s', target: 0 },   // Stop
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // More lenient during spikes
    http_req_failed: ['rate<0.1'],     // 10% error rate acceptable during spikes
  },
};

// Generate simple match data for spike testing
function generateQuickMatch() {
  const matchId = `spike-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  const teams = ['Spike-A', 'Spike-B', 'Spike-C', 'Spike-D'];
  
  return {
    id: matchId,
    teamA: teams[0],
    teamB: teams[1],
    teamAScore: 21,
    teamBScore: 19,
    winner: teams[0],
    matchDateTime: new Date().toISOString().slice(0, -1),
    gameNumber: 1
  };
}

export default function () {
  const matchResult = generateQuickMatch();
  
  const response = http.post(
    'http://host.docker.internal:8080/api/match-results',
    JSON.stringify(matchResult),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );
  
  check(response, {
    'spike test - status is 2xx': (r) => r.status >= 200 && r.status < 300,
    'spike test - response time < 5s': (r) => r.timings.duration < 5000,
  });
  
  // Minimal sleep during spike test
  sleep(0.1);
}

export function setup() {
  console.log('Starting spike test for Thomas Cup Kafka API');
}

export function teardown() {
  console.log('Spike test completed - check system recovery');
}