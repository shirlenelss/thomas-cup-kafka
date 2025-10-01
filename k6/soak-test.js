import http from 'k6/http';
import { check, sleep } from 'k6';

// Enhanced soak test - extended endurance with realistic load patterns
export const options = {
  stages: [
    { duration: '2m', target: 5 },    // Gradual ramp up
    { duration: '3m', target: 15 },   // Reach initial load
    { duration: '30m', target: 15 },  // Main soak period
    { duration: '3m', target: 25 },   // Mid-test load increase
    { duration: '10m', target: 25 },  // Sustained higher load
    { duration: '3m', target: 15 },   // Back to baseline
    { duration: '5m', target: 5 },    // Wind down
    { duration: '2m', target: 0 },    // Complete shutdown
  ],
  thresholds: {
    http_req_duration: ['p(50)<300', 'p(90)<800', 'p(95)<1200'], // Strict performance requirements
    http_req_failed: ['rate<0.01'],   // Very low error rate for stability
    checks: ['rate>0.95'],            // 95% of checks should pass
    'http_req_duration{phase:soak}': ['p(95)<1000'], // Main soak phase performance
    'http_req_duration{phase:increased}': ['p(95)<1500'], // Allow degradation during load increase
  },
  // Memory and connection management for long test
  noConnectionReuse: false,
  userAgent: 'Thomas-Cup-Soak-Test/1.0',
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
  
  // Determine test phase for performance tracking
  const elapsed = Math.floor((__VU + __ITER) / 100) % 4;
  const phases = ['ramp', 'soak', 'increased', 'wind-down'];
  const currentPhase = phases[elapsed] || 'soak';
  
  const response = http.post(
    'http://host.docker.internal:8080/api/match-results',
    JSON.stringify(matchResult),
    {
      headers: { 
        'Content-Type': 'application/json',
        'X-Test-Phase': currentPhase,
      },
      tags: { phase: currentPhase },
    }
  );
  
  const checks = check(response, {
    'soak - status is 200': (r) => r.status === 200,
    'soak - successful kafka response': (r) => r.body.includes('Match result sent to Kafka'),
    'soak - response time acceptable': (r) => r.timings.duration < 2000,
    'soak - response time good': (r) => r.timings.duration < 800,
    'soak - response time excellent': (r) => r.timings.duration < 300,
    'soak - no server errors': (r) => r.status < 500,
    'soak - proper content type': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
  }, { phase: currentPhase });
  
  // Periodic health check every 100 iterations
  if (__ITER % 100 === 0) {
    const healthResponse = http.get('http://host.docker.internal:8080/actuator/health');
    check(healthResponse, {
      'health check passes': (r) => r.status === 200 && r.json('status') === 'UP',
    });
  }
  
  // Log performance degradation
  if (response.timings.duration > 3000) {
    console.warn(`Slow response in ${currentPhase} phase: ${response.timings.duration}ms for ${matchResult.id}`);
  }
  
  // Realistic user behavior with some variation
  const baseThinkTime = currentPhase === 'increased' ? 3 : 4; // Faster during load increase
  sleep(Math.random() * baseThinkTime + 1); // 1-5 or 1-4 seconds based on phase
}

export function setup() {
  console.log('Starting 30-minute soak test for Thomas Cup Kafka');
  console.log('This will test system stability under sustained load');
}

export function teardown() {
  console.log('Soak test completed - check for memory leaks and performance degradation');
}