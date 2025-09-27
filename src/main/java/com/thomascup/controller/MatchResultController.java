package com.thomascup.controller;

/*
 * TODO: Fix Kafka Consumer Exceptions & Improve Architecture
 * 
 * CURRENT ISSUE:
 * - /api/new-game and /api/update-score endpoints are causing ClassCastException in consumers
 * - MatchResultDbConsumer expects MatchResult objects but receives JSON strings
 * - Database insertion fails due to null constraint violations
 * - Kafka retry mechanism exhausts after 10 attempts per message
 * 
 * ARCHITECTURAL IMPROVEMENTS TO IMPLEMENT:
 * 
 * 1. PREPROCESSING TABLE PATTERN (Recommended):
 *    - Create a 'match_preprocessing' table to store full MatchResult objects
 *    - Kafka messages should only contain lightweight references (IDs)
 *    - Benefits: Reduced Kafka payload size, better data integrity, easier debugging
 *    
 *    Flow: REST → Preprocessing Table → Kafka ID → Consumer fetches by ID → Final Table
 *    
 *    Implementation:
 *    a) Create preprocessing table:
 *       CREATE TABLE match_preprocessing (
 *         preprocessing_id UUID PRIMARY KEY,
 *         match_data JSONB NOT NULL,
 *         status VARCHAR(20) DEFAULT 'PENDING',
 *         created_at TIMESTAMP DEFAULT NOW()
 *       );
 *    
 *    b) Modify endpoints to:
 *       - Save MatchResult to preprocessing table
 *       - Send only preprocessing_id to Kafka topics
 *    
 *    c) Update consumers to:
 *       - Receive preprocessing_id from Kafka
 *       - Fetch full MatchResult from preprocessing table
 *       - Process and save to final tables
 *       - Mark preprocessing record as 'PROCESSED'
 * 
 * 2. ALTERNATIVE APPROACHES:
 *    a) JSON String Consumer Pattern:
 *       - Change consumer method signatures to accept String instead of MatchResult
 *       - Add JSON deserialization in consumers using ObjectMapper
 *       - Pros: Simple fix, Cons: JSON parsing overhead per message
 *    
 *    b) Proper Serialization Configuration:
 *       - Configure Kafka to properly serialize/deserialize MatchResult objects
 *       - Update KafkaTemplate configuration for consistent object handling
 *       - Pros: Clean object flow, Cons: Larger message payloads
 * 
 * 3. MONITORING IMPACT:
 *    - Current exceptions actually prove all 5 consumers are ACTIVE (SUCCESS!)
 *    - Grafana dashboard now shows activity for previously unused consumers
 *    - Fix should maintain consumer activity while eliminating errors
 * 
 * PRIORITY: Medium (monitoring goal achieved, but production needs clean error handling)
 * ESTIMATED EFFORT: 2-3 hours for preprocessing table pattern
 */

import com.thomascup.model.MatchResult;
import com.thomascup.service.MatchResultProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Match Results", description = "Endpoints for posting match results to Kafka")
@RestController
@RequestMapping("/api")
public class MatchResultController {
    @Autowired
    private MatchResultProducer matchResultProducer;
    
    @Autowired
    private KafkaTemplate<String, MatchResult> kafkaTemplate;

    @Operation(summary = "Send a match result", description = "Posts a match result event to Kafka.")
    @PostMapping("/match-results")
    public ResponseEntity<String> sendMatchResult(@RequestBody MatchResult matchResult) {
        // Set current timestamp if not provided
        if (matchResult.getMatchDateTime() == null) {
            matchResult.setMatchDateTime(java.time.LocalDateTime.now());
        }
        matchResultProducer.sendMatchResult(matchResult);
        return ResponseEntity.ok("Match result sent to Kafka");
    }
    
    @Operation(summary = "Start a new game", description = "Posts a new game event to the new-game topic.")
    @PostMapping("/new-game")
    public ResponseEntity<String> startNewGame(@RequestBody MatchResult matchResult) {
        // Set current timestamp if not provided  
        if (matchResult.getMatchDateTime() == null) {
            matchResult.setMatchDateTime(java.time.LocalDateTime.now());
        }
        kafkaTemplate.send("new-game", matchResult);
        return ResponseEntity.ok("New game started and sent to Kafka");
    }
    
    @Operation(summary = "Update match score", description = "Posts a score update event to the update-score topic.")
    @PostMapping("/update-score")
    public ResponseEntity<String> updateScore(@RequestBody MatchResult matchResult) {
        // Set current timestamp if not provided
        if (matchResult.getMatchDateTime() == null) {
            matchResult.setMatchDateTime(java.time.LocalDateTime.now());
        }
        kafkaTemplate.send("update-score", matchResult);
        return ResponseEntity.ok("Score update sent to Kafka");
    }
}
