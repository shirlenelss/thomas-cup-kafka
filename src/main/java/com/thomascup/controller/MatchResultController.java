package com.thomascup.controller;

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
