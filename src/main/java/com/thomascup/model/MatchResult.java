package com.thomascup.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.Data;
import lombok.Setter;

/**
 * This class is now deprecated. Use MatchHead and MatchScores instead.
 */
@Data
public class MatchResult {
    private String id; // Unique identifier for idempotency
    private String teamA;
    private String teamB;
    private int teamAScore;
    private int teamBScore;
    private String winner;
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private java.time.LocalDateTime matchDateTime;
    @Setter
    private int gameNumber; // 1, 2, or 3

    public MatchResult(String id, String teamA, String teamB, int teamAScore, int teamBScore, String winner, java.time.LocalDateTime matchDateTime, int gameNumber) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.winner = winner;
        this.matchDateTime = matchDateTime;
        this.gameNumber = gameNumber;
        validateGameNumber(gameNumber)
            .setTeamAScore(teamAScore)
            .setTeamBScore(teamBScore);
    }

    public MatchResult() {
        // Default constructor for Jackson
    }

    // Explicit getters/setters to avoid relying solely on Lombok for these
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public java.time.LocalDateTime getMatchDateTime() { return matchDateTime; }
    public void setMatchDateTime(java.time.LocalDateTime matchDateTime) { this.matchDateTime = matchDateTime; }
    public MatchResult validateGameNumber(int gameNumber) {
        // Validate that gameNumber is 1, 2, or 3
        if (!(gameNumber > 0 && gameNumber < 4)) {
            throw new IllegalArgumentException("Game number must be 1, 2, or 3 (badminton match is best of 3 games) but is " + gameNumber);
        }
        this.gameNumber = gameNumber;
        return this;
    }

    public void validate() {

        int maxPoints = (gameNumber == 3) ? 15 : 21;
        if (teamAScore < 0 || teamBScore < 0) {
            throw new IllegalArgumentException("Scores must be non-negative");
        }
        int cap = 30;
        if (teamAScore > cap || teamBScore > cap) {
            throw new IllegalArgumentException("Scores must not exceed 30 (badminton rules)");
        }
        if ((teamAScore > maxPoints && teamAScore < cap) || (teamBScore > maxPoints && teamBScore < cap)) {
            throw new IllegalArgumentException("Scores must not exceed " + maxPoints + " unless deuce up to 30");
        }
    }

    public MatchResult setTeamAScore(int teamAScore) {
        this.teamAScore = teamAScore;
        validate();
        return this;
    }

    public MatchResult setTeamBScore(int teamBScore) {
        this.teamBScore = teamBScore;
        validate();
        return this;
    }

    // Explicit getters for fields used in services
    public String getTeamA() { return teamA; }
    public String getTeamB() { return teamB; }
    public int getTeamAScore() { return teamAScore; }
    public int getTeamBScore() { return teamBScore; }
    public String getWinner() { return winner; }
    public int getGameNumber() { return gameNumber; }
}
