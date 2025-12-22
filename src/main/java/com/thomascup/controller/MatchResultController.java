package com.thomascup.controller;

import com.thomascup.model.MatchResult;
import com.thomascup.service.MatchResultProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

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
        // Generate ID if not provided
        if (matchResult.getId() == null || matchResult.getId().trim().isEmpty()) {
            matchResult.setId(UUID.randomUUID().toString());
        }
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
        if (matchResult.getId() == null || matchResult.getId().trim().isEmpty()) {
            matchResult.setId(UUID.randomUUID().toString());
        }
        if (matchResult.getMatchDateTime() == null) {
            matchResult.setMatchDateTime(java.time.LocalDateTime.now());
        }
        String key = matchResult.getId() + ":" + matchResult.getGameNumber();
        kafkaTemplate.send("new-game", key, matchResult);
        return ResponseEntity.ok("New game started and sent to Kafka");
    }
    
    @Operation(summary = "Update match score", description = "Posts a score update event to the update-score topic.")
    @PostMapping("/update-score")
    public ResponseEntity<String> updateScore(@RequestBody MatchResult matchResult) {
        if (matchResult.getId() == null || matchResult.getId().trim().isEmpty()) {
            matchResult.setId(UUID.randomUUID().toString());
        }
        if (matchResult.getMatchDateTime() == null) {
            matchResult.setMatchDateTime(java.time.LocalDateTime.now());
        }
        String key = matchResult.getId() + ":" + matchResult.getGameNumber();
        kafkaTemplate.send("update-score", key, matchResult);

        // If this update ends the game, also emit the final match result to the main topic
        if (isGameOver(matchResult)) {
            // Ensure winner is set
            if (matchResult.getWinner() == null || matchResult.getWinner().isBlank()) {
                if (matchResult.getTeamAScore() > matchResult.getTeamBScore()) {
                    matchResult.setWinner(matchResult.getTeamA());
                } else if (matchResult.getTeamBScore() > matchResult.getTeamAScore()) {
                    matchResult.setWinner(matchResult.getTeamB());
                }
            }
            matchResultProducer.sendMatchResult(matchResult);
        }
        return ResponseEntity.ok("Score update sent to Kafka");
    }

    private boolean isGameOver(MatchResult m) {
        int maxPoints = (m.getGameNumber() == 3) ? 15 : 21;
        int a = m.getTeamAScore();
        int b = m.getTeamBScore();
        int cap = 30;
        if (a >= cap || b >= cap) return true;
        return (a >= maxPoints || b >= maxPoints) && Math.abs(a - b) >= 2;
    }
}
