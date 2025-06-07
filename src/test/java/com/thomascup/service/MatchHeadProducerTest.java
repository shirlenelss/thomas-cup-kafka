package com.thomascup.service;

import com.thomascup.model.MatchHead;
import com.thomascup.model.MatchScores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class MatchHeadProducerTest {
    @Mock
    private KafkaTemplate<String, MatchHead> kafkaTemplate;

    @InjectMocks
    private MatchHeadProducer matchHeadProducer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testSendMatchHead() {
        MatchScores scores = new MatchScores(1, 21, 19, "TeamA");
        MatchHead head = new MatchHead(
            "match-1",
            "TeamA",
            "TeamB",
            LocalDateTime.now(),
            Collections.singletonList(scores)
        );
        matchHeadProducer.sendMatchHead(head);
        verify(kafkaTemplate).send("thomas-cup-matches", head);
    }

    @Test
    public void testIdempotency() {
        MatchScores scores1 = new MatchScores(1, 21, 19, "TeamA");
        MatchHead head1 = new MatchHead("match-1", "TeamA", "TeamB", LocalDateTime.now(), List.of(scores1));
        matchHeadProducer.sendMatchHead(head1);
        matchHeadProducer.sendMatchHead(head1); // Should not send again
        verify(kafkaTemplate, times(1)).send("thomas-cup-matches", head1);
    }
}

