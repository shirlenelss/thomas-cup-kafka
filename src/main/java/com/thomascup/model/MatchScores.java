package com.thomascup.model;

import lombok.Data;

@Data
public class MatchScores {
    private int gameNumber; // 1, 2, or 3
    private int teamAScore;
    private int teamBScore;
    private String winner;

    public MatchScores(int gameNumber, int teamAScore, int teamBScore, String winner) {
        validateScore(gameNumber, teamAScore, teamBScore);
        this.gameNumber = gameNumber;
        this.teamAScore = teamAScore;
        this.teamBScore = teamBScore;
        this.winner = winner;
    }

    public void setTeamAScore(int teamAScore) {
        validateScore(this.gameNumber, teamAScore, this.teamBScore);
        this.teamAScore = teamAScore;
    }

    public void setTeamBScore(int teamBScore) {
        validateScore(this.gameNumber, this.teamAScore, teamBScore);
        this.teamBScore = teamBScore;
    }

    public void setGameNumber(int gameNumber) {
        validateScore(gameNumber, this.teamAScore, this.teamBScore);
        this.gameNumber = gameNumber;
    }

    private void validateScore(int gameNumber, int teamAScore, int teamBScore) {
        if (!(gameNumber > 0 && gameNumber < 4)) {
            throw new IllegalArgumentException("Game number must be 1, 2, or 3 (badminton match is best of 3 games) but is " + gameNumber);
        }
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
}
