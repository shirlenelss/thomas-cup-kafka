package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchResultProducer {
    private static final String TOPIC = "thomas-cup-matches";

    @Autowired
    private KafkaTemplate<String, MatchResult> kafkaTemplate;

    public void sendMatchResult(MatchResult matchResult) {
        kafkaTemplate.send(TOPIC, matchResult);
    }
}

