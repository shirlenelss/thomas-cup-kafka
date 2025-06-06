package com.thomascup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {
    private String teamA;
    private String teamB;
    private int teamAScore;
    private int teamBScore;
    private String winner;
}
