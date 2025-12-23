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

// Base URL via env (works inside Docker with host.docker.internal)
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
// Debug/simulation mode: if set, only print payloads, no HTTP calls
const SIMULATE = (__ENV.SIMULATE === '1') || (__ENV.DRY_RUN === '1');

const teamPool = [
  'Malaysia', 'Indonesia', 'China', 'Japan', 'Denmark', 'Taiwan',
  'Thailand', 'India', 'Korea', 'England', 'Singapore', 'Vietnam'
];

function pickTeams() {
  const teamA = teamPool[Math.floor(Math.random() * teamPool.length)];
  let teamB = teamPool[Math.floor(Math.random() * teamPool.length)];
  while (teamB === teamA) teamB = teamPool[Math.floor(Math.random() * teamPool.length)];
  return { teamA, teamB };
}

// Generate a valid badminton game score sequence under official rules
function simulateGameSequence(gameNumber) {
  const maxPoints = gameNumber === 3 ? 15 : 21;
  const cap = 30;
  let a = 0, b = 0;
  const sequence = [{ a, b }]; // include starting 0-0

  // Random bias per game to avoid symmetric flip-flop
  const bias = 0.5 + (Math.random() - 0.5) * 0.2; // 0.4..0.6

  while (true) {
    if (Math.random() < bias) a++; else b++;
    sequence.push({ a, b });

    const reachedBase = (a >= maxPoints || b >= maxPoints) && Math.abs(a - b) >= 2;
    const reachedCap = a === cap || b === cap;
    if (reachedBase || reachedCap) return sequence;
  }
}

function buildMatchScenario() {
  const { teamA, teamB } = pickTeams();
  // Keep variety, but you can switch to modulo progression if desired
  const gameNumber = Math.floor(Math.random() * 3) + 1; // 1..3
  const sequence = simulateGameSequence(gameNumber); // [{a,b}, ...]
  const last = sequence[sequence.length - 1];
  const winner = last.a > last.b ? teamA : teamB;
  const id = `soak-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  const matchDateTime = new Date().toISOString().slice(0, -1);
  return { id, teamA, teamB, gameNumber, sequence, winner, matchDateTime };
}

export default function () {
  // Determine test phase for performance tracking (kept from original)
  const elapsed = Math.floor((__VU + __ITER) / 100) % 4;
  const phases = ['ramp', 'soak', 'increased', 'wind-down'];
  const currentPhase = phases[elapsed] || 'soak';

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

  // If simulation mode, print all payloads and return without HTTP
  if (SIMULATE) {
    console.log('SIMULATED new-game payload:', JSON.stringify(newGame));
    console.log(`SIMULATED update-score sequence (${sim.sequence.length - 1} events):`);
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

  // Send new-game
  const resNew = http.post(
    `${BASE_URL}/api/new-game`,
    JSON.stringify(newGame),
    {
      headers: { 'Content-Type': 'application/json', 'X-Test-Phase': currentPhase },
      tags: { phase: currentPhase },
    }
  );

  const ngOk = check(resNew, {
    'soak:new-game status is 200': (r) => r.status === 200,
    'soak:new-game body mentions started': (r) => String(r.body || '').toLowerCase().includes('new game'),
  }, { phase: currentPhase });

  if (!ngOk) {
    // brief backoff to avoid hot loop on failures
    sleep(0.5);
    return;
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

    const resUpd = http.post(
      `${BASE_URL}/api/update-score`,
      JSON.stringify(payload),
      {
        headers: { 'Content-Type': 'application/json', 'X-Test-Phase': currentPhase },
        tags: { phase: currentPhase },
      }
    );

    const ok = check(resUpd, {
      'soak:update-score status is 200/201': (r) => r.status === 200 || r.status === 201,
      'soak:update-score body ok': (r) => String(r.body || '').toLowerCase().includes('update') || r.status === 201,
      'soak:no server errors': (r) => r.status < 500,
    }, { phase: currentPhase });

    if (!ok) break;

    // small think-time between points
    sleep(0.05 + Math.random() * 0.1);
  }

  // Periodic health check every 100 iterations
  if (__ITER % 100 === 0) {
    const healthResponse = http.get(`${BASE_URL}/actuator/health`);
    check(healthResponse, {
      'health check passes': (r) => r.status === 200 && (r.json('status') === 'UP' || String(r.body).includes('UP')),
    }, { phase: currentPhase });
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