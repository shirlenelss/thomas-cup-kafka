package com.thomascup.service;

import com.thomascup.model.MatchHead;
import com.thomascup.model.MatchScores;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchHeadProducer {
    private static final String TOPIC = "thomas-cup-matches";

    @Autowired
    private KafkaTemplate<String, MatchHead> kafkaTemplate;

    // In-memory store for idempotency: id+gameNumber -> last processed MatchHead
    private final Map<String, MatchHead> latestHeads = new ConcurrentHashMap<>();

    public void sendMatchHead(MatchHead matchHead) {
        // Use id as key
        String key = matchHead.getId();
        MatchHead last = latestHeads.get(key);
        boolean shouldSend = false;
        if (last == null) {
            shouldSend = true;
        } else if (matchHead.getMatchDateTime() != null && last.getMatchDateTime() != null) {
            // Only send if the new event is newer or the scores have changed
            shouldSend = matchHead.getMatchDateTime().isAfter(last.getMatchDateTime()) ||
                !matchHead.getScores().equals(last.getScores());
        }
        if (shouldSend) {
            kafkaTemplate.send(TOPIC, matchHead);
            latestHeads.put(key, matchHead);
        }
    }
}

