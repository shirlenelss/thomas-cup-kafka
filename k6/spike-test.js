import http from 'k6/http';
import { check, sleep } from 'k6';

// Enhanced spike test - multiple traffic bursts to test system resilience
export const options = {
  stages: [
    { duration: '30s', target: 5 },    // Baseline
    { duration: '15s', target: 150 },  // First spike
    { duration: '1m', target: 150 },   // Sustained spike
    { duration: '15s', target: 10 },   // Recovery period
    { duration: '30s', target: 10 },   // Stable
    { duration: '10s', target: 200 },  // Second spike (higher)
    { duration: '45s', target: 200 },  // Peak stress
    { duration: '30s', target: 20 },   // Gradual recovery
    { duration: '30s', target: 0 },    // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(90)<3000', 'p(95)<5000'], // Lenient during stress
    http_req_failed: ['rate<0.15'],     // 15% error rate acceptable during spikes
    'http_req_duration{spike:first}': ['p(95)<4000'],
    'http_req_duration{spike:second}': ['p(95)<6000'],
  },
  // Spike test should handle higher resource usage
  maxRedirects: 0,
  batch: 10, // Process requests in batches during high load
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
  
  // Add spike context tags for detailed analysis
  const currentVU = __VU;
  const currentIter = __ITER;
  const spikePhase = currentVU > 100 ? 'second' : 'first';
  
  const response = http.post(
    'http://host.docker.internal:8080/api/match-results',
    JSON.stringify(matchResult),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { spike: spikePhase },
    }
  );
  
  const checks = check(response, {
    'spike - status is 2xx': (r) => r.status >= 200 && r.status < 300,
    'spike - not server error': (r) => r.status < 500,
    'spike - response within 10s': (r) => r.timings.duration < 10000,
    'spike - response within 5s': (r) => r.timings.duration < 5000,
    'spike - response within 2s': (r) => r.timings.duration < 2000,
    'spike - has response body': (r) => r.body && r.body.length > 0,
  }, { spike: spikePhase });
  
  // Log failures during stress periods
  if (!checks['spike - status is 2xx'] || !checks['spike - response within 10s']) {
    console.warn(`Spike ${spikePhase} - VU${currentVU} Iter${currentIter}: ${response.status} in ${response.timings.duration}ms`);
  }
  
  // Aggressive load during spike phases
  if (currentVU > 50) {
    sleep(0.05); // Very minimal delay during high load
  } else {
    sleep(0.2);  // Slightly longer during normal periods
  }
}

export function setup() {
  console.log('Starting spike test for Thomas Cup Kafka API');
}

export function teardown() {
  console.log('Spike test completed - check system recovery');
}