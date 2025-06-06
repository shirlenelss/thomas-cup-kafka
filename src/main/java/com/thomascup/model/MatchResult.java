package com.thomascup.model;

import lombok.Data;

@Data
public class MatchResult {
    private String id; // Unique identifier for idempotency
    private String teamA;
    private String teamB;
    private int teamAScore;
    private int teamBScore;
    private String winner;
    private java.time.LocalDateTime matchDateTime;
    private int gameNumber; // 1, 2, or 3

    public MatchResult(String id, String teamA, String teamB, int teamAScore, int teamBScore, String winner, java.time.LocalDateTime matchDateTime, int gameNumber) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.winner = winner;
        this.matchDateTime = matchDateTime;
        this.gameNumber = gameNumber;
        this.setTeamAScore(teamAScore);
        this.setTeamBScore(teamBScore);

    }

    public void setTeamAScore(int teamAScore) {
        this.teamAScore = teamAScore;
        validateScore(teamAScore, this.teamBScore, this.gameNumber);
    }

    public void setTeamBScore(int teamBScore) {
        this.teamBScore = teamBScore;
        validateScore(this.teamAScore, teamBScore, this.gameNumber);
    }

    private void validateScore(int teamAScore, int teamBScore, int gameNumber) {
        if (!(gameNumber > 0 && gameNumber < 4)) {
            throw new IllegalArgumentException("Game number must be 1, 2, or 3 (badminton match is best of 3 games) but is " + gameNumber);
        }
        int maxPoints = (gameNumber == 3) ? 15 : 21;
        int cap = 30;
        if (teamAScore < 0 || teamBScore < 0) {
            throw new IllegalArgumentException("Scores must be non-negative");
        }
        if (teamAScore > cap || teamBScore > cap) {
            throw new IllegalArgumentException("Scores must not exceed 30 (badminton rules)");
        }
        if ((teamAScore > maxPoints && teamAScore < cap) || (teamBScore > maxPoints && teamBScore < cap)) {
            throw new IllegalArgumentException("Scores must not exceed " + maxPoints + " unless deuce up to 30");
        }
    }
}
