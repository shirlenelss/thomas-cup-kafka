package com.thomascup.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Server-side counterparts of the custom metrics measured by the k6 tests
 * (match_results_sent, new_games_sent, score_updates_sent, games_by_number, kafka_errors).
 * Exposed via /actuator/prometheus with a "thomascup_" prefix.
 */
@Component
public class ApiMetrics {
    private final MeterRegistry registry;

    public ApiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Counts a message accepted by the API for a topic and game number. */
    public void messageReceived(String topic, int gameNumber) {
        Counter.builder("thomascup.api.messages")
                .description("Messages accepted by the API, per Kafka topic and game number")
                .tag("topic", topic)
                .tag("game", "game_" + gameNumber)
                .register(registry)
                .increment();
    }

    /** Counts a game that reached a terminal score via update-score. */
    public void gameCompleted(int gameNumber) {
        Counter.builder("thomascup.games.completed")
                .description("Games that reached a terminal score")
                .tag("game", "game_" + gameNumber)
                .register(registry)
                .increment();
    }

    /**
     * Tracks the outcome of an asynchronous Kafka send. A null future means the
     * producer skipped the send as a duplicate.
     */
    public void trackSend(String topic, CompletableFuture<?> future) {
        if (future == null) {
            Counter.builder("thomascup.kafka.send.skipped")
                    .description("Sends skipped by producer idempotency checks")
                    .tag("topic", topic)
                    .register(registry)
                    .increment();
            return;
        }
        future.whenComplete((result, ex) -> Counter.builder("thomascup.kafka.send")
                .description("Kafka send outcomes")
                .tag("topic", topic)
                .tag("result", ex == null ? "success" : "failure")
                .register(registry)
                .increment());
    }
}
