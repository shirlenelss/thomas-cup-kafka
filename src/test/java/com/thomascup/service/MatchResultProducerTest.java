package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

public class MatchResultProducerTest {
    @Mock
    private KafkaTemplate<String, MatchResult> kafkaTemplate;

    @InjectMocks
    private MatchResultProducer matchResultProducer;

    public MatchResultProducerTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSendMatchResult() {
        MatchResult result = new MatchResult(
            "match-1", // id
            "TeamA", // teamA
            "TeamB", // teamB
            3, // teamAScore
            2, // teamBScore
            "TeamA", // winner
            java.time.LocalDateTime.now() // matchDateTime
        );
        matchResultProducer.sendMatchResult(result);
        verify(kafkaTemplate).send("thomas-cup-matches", result);
    }

    @Test
    public void testIdempotency() {
        MatchResult result1 = new MatchResult(
            "match-1",
            "TeamA",
            "TeamB",
            3,
            2,
            "TeamA",
            java.time.LocalDateTime.of(2025, 6, 6, 10, 0)
        );
        MatchResult result2 = new MatchResult(
            "match-1",
            "TeamA",
            "TeamB",
            3,
            2,
            "TeamA",
            java.time.LocalDateTime.of(2025, 6, 6, 10, 0)
        );
        matchResultProducer.sendMatchResult(result1);
        matchResultProducer.sendMatchResult(result2);
        // Should only send once for identical id and matchDateTime
        verify(kafkaTemplate).send("thomas-cup-matches", result1);
    }

    @Test
    public void testScoreUpdateTriggersSend() {
        MatchResult result1 = new MatchResult(
            "match-1",
            "TeamA",
            "TeamB",
            3,
            2,
            "TeamA",
            java.time.LocalDateTime.of(2025, 6, 6, 10, 0)
        );
        MatchResult result2 = new MatchResult(
            "match-1",
            "TeamA",
            "TeamB",
            4, // score changed
            2,
            "TeamA",
            java.time.LocalDateTime.of(2025, 6, 6, 10, 0)
        );
        matchResultProducer.sendMatchResult(result1);
        matchResultProducer.sendMatchResult(result2);
        // Should send both times because the score changed
        verify(kafkaTemplate).send("thomas-cup-matches", result1);
        verify(kafkaTemplate).send("thomas-cup-matches", result2);
    }
}
