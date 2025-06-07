package com.thomascup.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MatchHead {
    private String id; // Unique identifier for idempotency
    private String teamA;
    private String teamB;
    private LocalDateTime matchDateTime;
    private List<MatchScores> scores;

    public MatchHead(String id, String teamA, String teamB, LocalDateTime matchDateTime, List<MatchScores> scores) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.matchDateTime = matchDateTime;
        this.scores = scores;
    }
}

