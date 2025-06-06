package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class MatchResultConsumerTest {
    @Test
    public void testListenGroup1LogsMessage() {
        Logger logger = mock(Logger.class);
        MatchResult matchResult = new MatchResult("match-1", "TeamA", "TeamB", 3, 2, "TeamA", LocalDateTime.now(), 1);
        ConsumerRecord<String, MatchResult> record = new ConsumerRecord<>(
                "thomas-cup-matches", 1, 0L, "match-1", matchResult);

        MatchResultConsumer consumer = new MatchResultConsumerWithLogger(logger);
        consumer.listenGroup1(record);
        verify(logger).info(Mockito.contains("[Group-1] Received message:"), Mockito.eq(matchResult), Mockito.eq(1));
    }

    @Test
    public void testListenGroup2LogsMessage() {
        Logger logger = mock(Logger.class);
        MatchResult matchResult = new MatchResult("match-2", "TeamC", "TeamD", 1, 4, "TeamD", LocalDateTime.now(), 1);
        ConsumerRecord<String, MatchResult> record = new ConsumerRecord<>(
                "thomas-cup-matches", 0, 0L, "match-2", matchResult);

        MatchResultConsumer consumer = new MatchResultConsumerWithLogger(logger);
        consumer.listenGroup2(record);
        verify(logger).info(Mockito.contains("[Group-2] Received message:"), Mockito.eq(matchResult), Mockito.eq(0));
    }

    // Helper class to inject mock logger
    static class MatchResultConsumerWithLogger extends MatchResultConsumer {
        private final Logger logger;
        MatchResultConsumerWithLogger(Logger logger) { this.logger = logger; }
        @Override
        protected Logger getLogger() { return logger; }
    }
}
