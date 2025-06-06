package com.thomascup.controller;

import com.thomascup.model.MatchResult;
import com.thomascup.service.MatchResultProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match-results")
public class MatchResultController {
    @Autowired
    private MatchResultProducer matchResultProducer;

    @PostMapping
    public ResponseEntity<String> sendMatchResult(@RequestBody MatchResult matchResult) {
        matchResultProducer.sendMatchResult(matchResult);
        return ResponseEntity.ok("Match result sent to Kafka");
    }
}

