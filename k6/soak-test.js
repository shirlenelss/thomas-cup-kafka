import http from 'k6/http';
import { check, sleep } from 'k6';

// Soak test - extended duration with moderate load
export const options = {
  stages: [
    { duration: '5m', target: 10 },  // Ramp up
    { duration: '30m', target: 10 }, // Soak test - 30 minutes at 10 users
    { duration: '5m', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.02'],
  },
};

const teamPool = [
  'Malaysia', 'Indonesia', 'China', 'Japan', 'Denmark', 'Taiwan',
  'Thailand', 'India', 'Korea', 'England', 'Singapore', 'Vietnam'
];

function generateSoakTestMatch() {
  const teamA = teamPool[Math.floor(Math.random() * teamPool.length)];
  let teamB = teamPool[Math.floor(Math.random() * teamPool.length)];
  while (teamB === teamA) {
    teamB = teamPool[Math.floor(Math.random() * teamPool.length)];
  }
  
  // Generate realistic 3-game match progression
  const gameNumber = (Math.floor(Date.now() / 1000) % 3) + 1;
  const maxPoints = gameNumber === 3 ? 15 : 21;
  
  // Simulate progressive match intensity
  let teamAScore = Math.floor(Math.random() * (maxPoints + 1));
  let teamBScore = Math.floor(Math.random() * (maxPoints + 1));
  
  // Ensure valid badminton score
  if (teamAScore === teamBScore && teamAScore >= maxPoints) {
    teamAScore = Math.min(teamAScore + 2, 30);
  }
  
  return {
    id: `soak-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
    teamA,
    teamB,
    teamAScore,
    teamBScore,
    winner: teamAScore > teamBScore ? teamA : teamB,
    matchDateTime: new Date().toISOString().slice(0, -1),
    gameNumber
  };
}

export default function () {
  const matchResult = generateSoakTestMatch();
  
  const response = http.post(
    'http://host.docker.internal:8080/api/match-results',
    JSON.stringify(matchResult),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );
  
  check(response, {
    'soak test - status is 200': (r) => r.status === 200,
    'soak test - kafka response': (r) => r.body.includes('Match result sent to Kafka'),
    'soak test - response time ok': (r) => r.timings.duration < 2000,
  });
  
  // Longer sleep for soak test to simulate realistic user behavior
  sleep(Math.random() * 5 + 2); // 2-7 seconds between requests
}

export function setup() {
  console.log('Starting 30-minute soak test for Thomas Cup Kafka');
  console.log('This will test system stability under sustained load');
}

export function teardown() {
  console.log('Soak test completed - check for memory leaks and performance degradation');
}