package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Deprecated // Use MatchHeadProducer instead
public class MatchResultProducer {
    private static final String TOPIC = "thomas-cup-matches";

    @Autowired
    private KafkaTemplate<String, MatchResult> kafkaTemplate;

    // In-memory store for idempotency: id+gameNumber -> last processed MatchResult
    private final Map<String, MatchResult> latestResults = new ConcurrentHashMap<>();

    public void sendMatchResult(MatchResult matchResult) {
        String key = matchResult.getId() + ":" + matchResult.getGameNumber();
        MatchResult last = latestResults.get(key);
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
            latestResults.put(key, matchResult);
        }
    }
}
