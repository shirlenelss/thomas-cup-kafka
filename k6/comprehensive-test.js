import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

// Enhanced custom metrics for comprehensive performance analysis
const badmintonMatchesCounter = new Counter('badminton_matches_sent');
const kafkaErrorRate = new Rate('kafka_errors');
const matchProcessingTime = new Trend('match_processing_duration');
const concurrentUsers = new Gauge('concurrent_users');
const apiResponseSize = new Trend('api_response_size_bytes');
const gameDistribution = new Counter('games_by_number');

// Comprehensive performance test - combines aspects of load, spike, and endurance
export const options = {
  stages: [
    // Phase 1: Warm-up
    { duration: '1m', target: 5 },
    
    // Phase 2: Gradual load increase
    { duration: '2m', target: 20 },
    { duration: '3m', target: 40 },
    
    // Phase 3: Sustained moderate load
    { duration: '5m', target: 60 },
    
    // Phase 4: Spike test
    { duration: '30s', target: 120 },
    { duration: '2m', target: 120 },
    { duration: '30s', target: 60 },
    
    // Phase 5: Recovery and endurance
    { duration: '8m', target: 40 },
    
    // Phase 6: Final spike
    { duration: '20s', target: 100 },
    { duration: '1m', target: 100 },
    
    // Phase 7: Cool down
    { duration: '2m', target: 20 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    // Response time requirements
    http_req_duration: ['p(50)<250', 'p(90)<600', 'p(95)<1000', 'p(99)<2000'],
    
    // Reliability requirements
    http_req_failed: ['rate<0.02'], // Less than 2% failure rate
    kafka_errors: ['rate<0.005'],   // Less than 0.5% Kafka errors
    
    // Performance requirements
    badminton_matches_sent: ['count>800'], // Minimum throughput
    match_processing_duration: ['p(90)<800'],
    
    // Resource requirements
    api_response_size_bytes: ['p(95)<1000'], // Response size efficiency
    
    // Phase-specific requirements
    'http_req_duration{phase:spike}': ['p(95)<3000'],
    'http_req_duration{phase:sustained}': ['p(95)<800'],
    'http_req_duration{phase:endurance}': ['p(95)<1000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)', 'count'],
  setupTimeout: '30s',
  teardownTimeout: '30s',
};

// Enhanced match result generation with tournament simulation
const tournaments = [
  'Thomas Cup 2024', 'Uber Cup 2024', 'BWF World Championships', 
  'All England Open', 'Indonesia Masters', 'Malaysia Open'
];

const countries = [
  'Malaysia', 'Indonesia', 'China', 'Japan', 'Denmark', 'Taiwan',
  'Thailand', 'India', 'Korea', 'England', 'Singapore', 'Vietnam',
  'Philippines', 'Hong Kong', 'Australia', 'Germany', 'France', 'Spain'
];

function generateRealisticMatch() {
  const tournament = tournaments[Math.floor(Math.random() * tournaments.length)];
  const teamA = countries[Math.floor(Math.random() * countries.length)];
  let teamB = countries[Math.floor(Math.random() * countries.length)];
  while (teamB === teamA) {
    teamB = countries[Math.floor(Math.random() * countries.length)];
  }
  
  const gameNumber = Math.floor(Math.random() * 3) + 1;
  const maxPoints = gameNumber === 3 ? 15 : 21;
  
  // Simulate different match intensities
  const matchTypes = ['decisive', 'close', 'deuce'];
  const matchType = matchTypes[Math.floor(Math.random() * matchTypes.length)];
  
  let teamAScore, teamBScore;
  
  switch (matchType) {
    case 'decisive':
      teamAScore = Math.floor(Math.random() * 5) + maxPoints;
      teamBScore = Math.floor(Math.random() * (maxPoints - 5));
      break;
    case 'close':
      const baseScore = maxPoints - 2;
      teamAScore = baseScore + Math.floor(Math.random() * 4);
      teamBScore = baseScore + Math.floor(Math.random() * 4);
      break;
    case 'deuce':
      teamAScore = Math.min(maxPoints + Math.floor(Math.random() * 10), 30);
      teamBScore = Math.min(maxPoints + Math.floor(Math.random() * 10), 30);
      // Ensure winning margin
      if (Math.abs(teamAScore - teamBScore) < 2 && Math.max(teamAScore, teamBScore) < 30) {
        teamAScore = teamAScore > teamBScore ? teamAScore + 2 : teamAScore;
        teamBScore = teamBScore > teamAScore ? teamBScore + 2 : teamBScore;
      }
      break;
  }
  
  // Track game distribution
  gameDistribution.add(1, { game: `game_${gameNumber}` });
  
  return {
    id: `perf-${tournament.replace(/\s+/g, '-').toLowerCase()}-${Date.now()}-${Math.floor(Math.random() * 10000)}`,
    teamA,
    teamB,
    teamAScore,
    teamBScore,
    winner: teamAScore > teamBScore ? teamA : teamB,
    matchDateTime: new Date().toISOString().slice(0, -1),
    gameNumber,
    tournament,
    matchType
  };
}

// Determine current test phase
function getCurrentPhase() {
  const totalDuration = 26 * 60; // 26 minutes in seconds
  const elapsed = (__VU + __ITER * 10) % totalDuration;
  
  if (elapsed < 180) return 'warmup';
  if (elapsed < 480) return 'ramp';
  if (elapsed < 780) return 'sustained';
  if (elapsed < 960) return 'spike';
  if (elapsed < 1440) return 'endurance';
  if (elapsed < 1500) return 'final-spike';
  return 'cooldown';
}

export default function () {
  const phase = getCurrentPhase();
  const matchResult = generateRealisticMatch();
  
  // Track concurrent users
  concurrentUsers.add(__VU);
  
  const payload = JSON.stringify(matchResult);
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Test-Phase': phase,
      'X-Tournament': matchResult.tournament,
    },
    tags: { 
      phase: phase,
      tournament: matchResult.tournament,
      match_type: matchResult.matchType,
      game_number: `game_${matchResult.gameNumber}`
    },
  };
  
  const startTime = Date.now();
  const response = http.post('http://host.docker.internal:8080/api/match-results', payload, params);
  const endTime = Date.now();
  
  // Record enhanced metrics
  badmintonMatchesCounter.add(1);
  matchProcessingTime.add(endTime - startTime);
  apiResponseSize.add(response.body ? response.body.length : 0);
  
  // Comprehensive response validation
  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'contains success message': (r) => r.body && r.body.includes('Match result sent to Kafka'),
    'response time < 500ms': (r) => r.timings.duration < 500,
    'response time < 1000ms': (r) => r.timings.duration < 1000,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
    'has proper headers': (r) => r.headers['Content-Type'] !== undefined,
    'body not empty': (r) => r.body && r.body.length > 0,
    'no server errors': (r) => r.status < 500,
    'response size reasonable': (r) => r.body && r.body.length < 1000,
  }, { 
    phase: phase,
    tournament: matchResult.tournament,
    match_type: matchResult.matchType 
  });
  
  if (!success) {
    kafkaErrorRate.add(1);
    console.error(`${phase.toUpperCase()} PHASE - Failed: ${matchResult.id} - ${response.status} - ${response.timings.duration}ms`);
  } else {
    kafkaErrorRate.add(0);
  }
  
  // Phase-specific behavior simulation
  let thinkTime;
  switch (phase) {
    case 'spike':
    case 'final-spike':
      thinkTime = 0.1 + Math.random() * 0.2; // 0.1-0.3s aggressive load
      break;
    case 'sustained':
    case 'endurance':
      thinkTime = 0.5 + Math.random() * 1.0; // 0.5-1.5s moderate load
      break;
    default:
      thinkTime = 1.0 + Math.random() * 2.0; // 1.0-3.0s normal load
  }
  
  // Adaptive behavior based on response time
  if (response.timings.duration > 1000) {
    thinkTime *= 1.5; // Back off if server is struggling
  }
  
  sleep(thinkTime);
}

export function setup() {
  console.log('🏸 Starting Thomas Cup Kafka Comprehensive Performance Test');
  console.log('================================================================');
  console.log('This test combines load, spike, and endurance testing');
  console.log('Duration: ~26 minutes with multiple test phases');
  console.log('');
  
  // Verify system health before starting
  const healthCheck = http.get('http://host.docker.internal:8080/actuator/health');
  if (healthCheck.status !== 200) {
    console.error('❌ API health check failed - ensure all services are running');
    console.error('   Run: ./scripts/dev.sh start');
    return;
  }
  
  console.log('✅ Pre-test health check passed');
  console.log('📊 Monitor results at: http://localhost:3001 (Grafana)');
  
  return { 
    startTime: Date.now(),
    testType: 'comprehensive-performance',
    expectedDuration: 26 * 60 * 1000 // 26 minutes
  };
}

export function teardown(data) {
  if (!data) return;
  
  const duration = (Date.now() - data.startTime) / 1000;
  const minutes = Math.floor(duration / 60);
  const seconds = Math.floor(duration % 60);
  
  console.log('');
  console.log('🏁 Thomas Cup Kafka Performance Test Completed');
  console.log('==============================================');
  console.log(`⏱️  Duration: ${minutes}m ${seconds}s`);
  console.log('📈 Check the following for detailed analysis:');
  console.log('   • Terminal output above for metrics summary');
  console.log('   • Grafana dashboard: http://localhost:3001');
  console.log('   • Prometheus metrics: http://localhost:9090');
  console.log('   • Application logs: ./scripts/dev.sh logs');
  console.log('');
  console.log('💡 Next steps:');
  console.log('   1. Review performance metrics and identify bottlenecks');
  console.log('   2. Check Kafka topic consumer lag');
  console.log('   3. Verify database connection pool usage');
  console.log('   4. Monitor JVM memory usage trends');
}