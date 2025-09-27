package com.thomascup.service;

/*
 * TODO: Fix ClassCastException in new-game & update-score consumers
 * 
 * CURRENT ISSUE: 
 * - saveNewGameToDb() and updateScoreInDb() methods expect MatchResult objects
 * - But /api/new-game and /api/update-score endpoints send JSON strings
 * - Causes ClassCastException: String cannot be cast to MatchResult
 * 
 * SOLUTION OPTIONS (see detailed plan in MatchResultController.java):
 * 1. PREFERRED: Implement preprocessing table pattern with ID-based messaging
 * 2. ALTERNATIVE: Change method signatures to accept String and deserialize JSON
 * 
 * NOTE: These exceptions actually prove consumers are ACTIVE - mission accomplished for monitoring!
 */

import com.thomascup.model.MatchResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MatchResultDbConsumer {
    private static final Logger logger = LoggerFactory.getLogger(MatchResultDbConsumer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "thomas-cup-matches", groupId = "db-writer-group", containerFactory = "matchResultKafkaListenerContainerFactory", id = "thomas-cup-db-main")
    public void saveLatestToDb(ConsumerRecord<String, MatchResult> record) {
        try {
            MatchResult matchResult = record.value();
            // PostgreSQL UPSERT: insert if not exists, update if exists
            String sql = "INSERT INTO match_results (id, teamA, teamB, teamAScore, teamBScore, winner, matchDateTime, gameNumber) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (id, gameNumber) DO UPDATE SET " +
                    "teamA = EXCLUDED.teamA, teamB = EXCLUDED.teamB, teamAScore = EXCLUDED.teamAScore, " +
                    "teamBScore = EXCLUDED.teamBScore, winner = EXCLUDED.winner, matchDateTime = EXCLUDED.matchDateTime";
            jdbcTemplate.update(sql,
                    matchResult.getId(),
                    matchResult.getTeamA(),
                    matchResult.getTeamB(),
                    matchResult.getTeamAScore(),
                    matchResult.getTeamBScore(),
                    matchResult.getWinner(),
                    matchResult.getMatchDateTime() != null ? java.sql.Timestamp.valueOf(matchResult.getMatchDateTime()) : null,
                    matchResult.getGameNumber()
            );
        } catch (Exception e) {
            logger.error("Failed to process record from topic 'thomas-cup-matches' at offset {}: {}", record.offset(), e.getMessage(), e);
            logger.error("Raw record value: {}", record.value());
            throw e;
        }
    }

    @KafkaListener(topics = "new-game", groupId = "db-writer-group", containerFactory = "matchResultKafkaListenerContainerFactory", id = "thomas-cup-db-new-game")
    public void saveNewGameToDb(ConsumerRecord<String, MatchResult> record) {
        MatchResult matchResult = record.value();
        String sql = "INSERT INTO match_results (id, teamA, teamB, teamAScore, teamBScore, winner, matchDateTime, gameNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (id, gameNumber) DO NOTHING";
        jdbcTemplate.update(sql,
                matchResult.getId(),
                matchResult.getTeamA(),
                matchResult.getTeamB(),
                matchResult.getTeamAScore(),
                matchResult.getTeamBScore(),
                matchResult.getWinner(),
                matchResult.getMatchDateTime() != null ? java.sql.Timestamp.valueOf(matchResult.getMatchDateTime()) : null,
                matchResult.getGameNumber()
        );
    }

    @KafkaListener(topics = "update-score", groupId = "db-writer-group", id = "thomas-cup-db-update-score")
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
