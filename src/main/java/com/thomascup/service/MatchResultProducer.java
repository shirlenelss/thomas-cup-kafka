package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchResultProducer {
    private static final String TOPIC = "thomas-cup-matches";

    @Autowired
    private KafkaTemplate<String, MatchResult> kafkaTemplate;

    // In-memory store for idempotency: id -> last processed MatchResult
    private final Map<String, MatchResult> latestResults = new ConcurrentHashMap<>();

    public void sendMatchResult(MatchResult matchResult) {
        MatchResult last = latestResults.get(matchResult.getId());
        boolean shouldSend = false;
        if (last == null) {
            shouldSend = true;
        } else if (matchResult.getMatchDateTime() != null && last.getMatchDateTime() != null) {
            // Only send if the new event is newer or the score has changed
            shouldSend = matchResult.getMatchDateTime().isAfter(last.getMatchDateTime()) ||
                matchResult.getTeamAScore() != last.getTeamAScore() ||
                matchResult.getTeamBScore() != last.getTeamBScore();
        }
        if (shouldSend) {
            kafkaTemplate.send(TOPIC, matchResult);
            latestResults.put(matchResult.getId(), matchResult);
        }
    }
}
