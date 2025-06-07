package com.thomascup.controller;

import com.thomascup.model.MatchResult;
import com.thomascup.service.MatchResultProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Match Results", description = "Endpoints for posting match results to Kafka")
@RestController
@RequestMapping("/api/match-results")
public class MatchResultController {
    @Autowired
    private MatchResultProducer matchResultProducer;

    @Operation(summary = "Send a match result", description = "Posts a match result event to Kafka.")
    @PostMapping
    public ResponseEntity<String> sendMatchResult(@RequestBody MatchResult matchResult) {
        matchResultProducer.sendMatchResult(matchResult);
        return ResponseEntity.ok("Match result sent to Kafka");
    }
}
