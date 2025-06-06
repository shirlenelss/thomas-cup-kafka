package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MatchResultDbConsumer {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "thomas-cup-matches", groupId = "db-writer-group")
    public void saveLatestToDb(ConsumerRecord<String, MatchResult> record) {
        MatchResult matchResult = record.value();
        // Upsert logic: update if exists, else insert
        String sql = "MERGE INTO match_results AS t USING (VALUES (?, ?, ?, ?, ?, ?, ?, ?)) AS v(id, teamA, teamB, teamAScore, teamBScore, winner, matchDateTime, gameNumber) " +
                "ON t.id = v.id AND t.gameNumber = v.gameNumber " +
                "WHEN MATCHED THEN UPDATE SET teamA = v.teamA, teamB = v.teamB, teamAScore = v.teamAScore, teamBScore = v.teamBScore, winner = v.winner, matchDateTime = v.matchDateTime " +
                "WHEN NOT MATCHED THEN INSERT (id, teamA, teamB, teamAScore, teamBScore, winner, matchDateTime, gameNumber) VALUES (v.id, v.teamA, v.teamB, v.teamAScore, v.teamBScore, v.winner, v.matchDateTime, v.gameNumber);";
        jdbcTemplate.update(sql,
                matchResult.getId(),
                matchResult.getTeamA(),
                matchResult.getTeamB(),
                matchResult.getTeamAScore(),
                matchResult.getTeamBScore(),
                matchResult.getWinner(),
                matchResult.getMatchDateTime(),
                matchResult.getGameNumber()
        );
    }

    @KafkaListener(topics = "new-game", groupId = "db-writer-group")
    public void saveNewGameToDb(ConsumerRecord<String, MatchResult> record) {
        MatchResult matchResult = record.value();
        String sql = "INSERT INTO match_results (id, teamA, teamB, teamAScore, teamBScore, winner, matchDateTime, gameNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id, gameNumber) DO NOTHING";
        jdbcTemplate.update(sql,
                matchResult.getId(),
                matchResult.getTeamA(),
                matchResult.getTeamB(),
                matchResult.getTeamAScore(),
                matchResult.getTeamBScore(),
                matchResult.getWinner(),
                matchResult.getMatchDateTime(),
                matchResult.getGameNumber()
        );
    }

    @KafkaListener(topics = "update-score", groupId = "db-writer-group")
    public void updateScoreInDb(ConsumerRecord<String, MatchResult> record) {
        MatchResult matchResult = record.value();
        String sql = "UPDATE match_results SET teamAScore = ?, teamBScore = ?, winner = ?, matchDateTime = ? WHERE id = ? AND gameNumber = ?";
        jdbcTemplate.update(sql,
                matchResult.getTeamAScore(),
                matchResult.getTeamBScore(),
                matchResult.getWinner(),
                matchResult.getMatchDateTime(),
                matchResult.getId(),
                matchResult.getGameNumber()
        );
    }
}
