package com.thomascup.config;

import com.thomascup.model.MatchResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, MatchResult> matchResultConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "db-writer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.thomascup.model");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.thomascup.model.MatchResult");
        
        // Production-ready consumer settings
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Process from beginning if no offset
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reliability
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 seconds session timeout
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds heartbeat
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Process up to 500 records per poll
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024); // Minimum bytes to fetch
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500); // Max wait for minimum bytes
        
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ErrorHandlingDeserializer<>(new JsonDeserializer<>(MatchResult.class)));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MatchResult> matchResultKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MatchResult> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(matchResultConsumerFactory());
        return factory;
    }
}
