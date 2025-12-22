package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public void saveNewGameToDb(ConsumerRecord<String, Object> record) {
        MatchResult matchResult = extractMatchResult(record.value());
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

    @KafkaListener(topics = "update-score", groupId = "db-writer-group", containerFactory = "matchResultKafkaListenerContainerFactory", id = "thomas-cup-db-update-score")
    public void updateScoreInDb(ConsumerRecord<String, Object> record) {
        MatchResult matchResult = extractMatchResult(record.value());
        String sql = "INSERT INTO match_results (id, teamA, teamB, teamAScore, teamBScore, winner, matchDateTime, gameNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (id, gameNumber) DO UPDATE SET teamAScore = EXCLUDED.teamAScore, teamBScore = EXCLUDED.teamBScore, winner = EXCLUDED.winner, matchDateTime = EXCLUDED.matchDateTime";
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

    /**
     * Helper method to extract MatchResult from either String (JSON) or MatchResult object
     */
    private MatchResult extractMatchResult(Object value) {
        if (value instanceof MatchResult) {
            return (MatchResult) value;
        } else if (value instanceof String) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.findAndRegisterModules(); // For LocalDateTime support
                return mapper.readValue((String) value, MatchResult.class);
            } catch (Exception e) {
                logger.error("Failed to deserialize JSON to MatchResult: {}", value, e);
                throw new RuntimeException("Invalid MatchResult JSON", e);
            }
        } else {
            throw new IllegalArgumentException("Expected MatchResult or String, got: " + 
                (value != null ? value.getClass().getSimpleName() : "null"));
        }
    }
}
