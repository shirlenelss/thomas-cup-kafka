package com.thomascup.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApiMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ApiMetrics metrics = new ApiMetrics(registry);

    @Test
    public void countsMessagesPerTopicAndGame() {
        metrics.messageReceived("new-game", 1);
        metrics.messageReceived("new-game", 1);
        metrics.messageReceived("update-score", 3);

        assertEquals(2.0, registry.get("thomascup.api.messages").tags("topic", "new-game", "game", "game_1").counter().count());
        assertEquals(1.0, registry.get("thomascup.api.messages").tags("topic", "update-score", "game", "game_3").counter().count());
    }

    @Test
    public void tracksSendOutcomes() {
        metrics.trackSend("update-score", CompletableFuture.completedFuture("ok"));
        metrics.trackSend("update-score", CompletableFuture.failedFuture(new RuntimeException("broker down")));
        metrics.trackSend("thomas-cup-matches", null);

        assertEquals(1.0, registry.get("thomascup.kafka.send").tags("topic", "update-score", "result", "success").counter().count());
        assertEquals(1.0, registry.get("thomascup.kafka.send").tags("topic", "update-score", "result", "failure").counter().count());
        assertEquals(1.0, registry.get("thomascup.kafka.send.skipped").tags("topic", "thomas-cup-matches").counter().count());
    }

    @Test
    public void countsCompletedGames() {
        metrics.gameCompleted(2);
        assertEquals(1.0, registry.get("thomascup.games.completed").tags("game", "game_2").counter().count());
    }
}
