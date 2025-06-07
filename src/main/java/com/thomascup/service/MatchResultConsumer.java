package com.thomascup.service;

import com.thomascup.model.MatchResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Deprecated
public class MatchResultConsumer {
    protected Logger getLogger() {
        return LoggerFactory.getLogger(MatchResultConsumer.class);
    }

    @KafkaListener(topics = "thomas-cup-matches", groupId = "group-1")
    public void listenGroup1(ConsumerRecord<String, MatchResult> record) {
        getLogger().info("[Group-1] Received message: {} from partition: {}", record.value(), record.partition());
    }

    @KafkaListener(topics = "thomas-cup-matches", groupId = "group-2")
    public void listenGroup2(ConsumerRecord<String, MatchResult> record) {
        getLogger().info("[Group-2] Received message: {} from partition: {}", record.value(), record.partition());
    }
}
