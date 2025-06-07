package com.thomascup.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomascup.model.MatchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
    }
)
@EmbeddedKafka(partitions = 1, topics = {"thomas-cup-matches"})
public class MatchResultIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private static RestTemplate restTemplate;

    @BeforeAll
    public static void setup() {
        restTemplate = new RestTemplate();
    }

    @AfterAll
    public static void tearDown() {
        restTemplate = null;
    }

    @Test
    public void testPostMatchResultAndConsume() throws Exception {
        MatchResult result = new MatchResult(
                "match-1",
                "TeamA",
                "TeamB",
                21,
                19,
                "TeamA",
                LocalDateTime.of(2025, 6, 7, 10, 0),
                1
        );
        String json = objectMapper.writeValueAsString(result);
        log.info("Posting MatchResult: {}", json);
        String url = "http://localhost:" + port + "/api/match-results";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        log.info("HTTP response: {} {}", response.getStatusCode(), response.getBody());
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Match result sent to Kafka");

        // Consume from Kafka
        Properties props = new Properties();
        props.put("bootstrap.servers", embeddedKafkaBroker.getBrokersAsString());
        props.put("group.id", "test-group");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", org.springframework.kafka.support.serializer.JsonDeserializer.class.getName());
        props.put("spring.json.trusted.packages", "*");
        props.put("spring.json.value.default.type", "com.thomascup.model.MatchResult");
        try (KafkaConsumer<String, MatchResult> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("thomas-cup-matches"));
            ConsumerRecord<String, MatchResult> record = null;
            long end = System.currentTimeMillis() + 10000; // wait up to 10 seconds
            while (System.currentTimeMillis() < end) {
                var records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    record = records.iterator().next();
                    log.info("Received Kafka record: {}", record.value());
                    break;
                }
            }
            assertThat(record).withFailMessage("No Kafka record received within timeout").isNotNull();
            assertThat(record.value().getId()).isEqualTo("match-1");
            assertThat(record.value().getTeamA()).isEqualTo("TeamA");
            assertThat(record.value().getTeamB()).isEqualTo("TeamB");
            assertThat(record.value().getTeamAScore()).isEqualTo(21);
            assertThat(record.value().getTeamBScore()).isEqualTo(19);
            assertThat(record.value().getWinner()).isEqualTo("TeamA");
            assertThat(record.value().getGameNumber()).isEqualTo(1);
        }
    }
}
