package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

public class MatchResultDbConsumerTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MatchResultDbConsumer consumer;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSaveNewGameToDb() {
        MatchResult matchResult = new MatchResult("match-1", "TeamA", "TeamB", 0, 0, null, LocalDateTime.now(), 1);
        ConsumerRecord<String, MatchResult> record = new ConsumerRecord<>("new-game", 0, 0L, "match-1", matchResult);
        consumer.saveNewGameToDb(record);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture());
        assertEquals("INSERT INTO match_results (id, teamA, teamB, teamAScore, teamBScore, winner, matchDateTime, gameNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id, gameNumber) DO NOTHING", sqlCaptor.getValue());
    }

    @Test
    public void testUpdateScoreInDb() {
        MatchResult matchResult = new MatchResult("match-1", "TeamA", "TeamB", 10, 8, null, LocalDateTime.now(), 1);
        ConsumerRecord<String, MatchResult> record = new ConsumerRecord<>("update-score", 0, 0L, "match-1", matchResult);
        consumer.updateScoreInDb(record);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> argCaptor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture(), argCaptor.capture());
        assertEquals("UPDATE match_results SET teamAScore = ?, teamBScore = ?, winner = ?, matchDateTime = ? WHERE id = ? AND gameNumber = ?", sqlCaptor.getValue());
    }
}

