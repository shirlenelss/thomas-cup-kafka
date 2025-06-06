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
        MatchResult result = new MatchResult("TeamA", "TeamB", 3, 2, "TeamA");
        matchResultProducer.sendMatchResult(result);
        verify(kafkaTemplate).send("thomas-cup-matches", result);
    }
}

