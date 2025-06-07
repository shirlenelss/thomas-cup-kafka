package com.thomascup.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomascup.model.MatchResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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

    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private static RestTemplate restTemplate;

    @BeforeEach
    void initBroker(ApplicationContext context) {
        this.embeddedKafkaBroker = context.getBean(EmbeddedKafkaBroker.class);
    }

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

        // 🛠 Updated Kafka Consumer Config
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MatchResult.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        try (KafkaConsumer<String, MatchResult> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList("thomas-cup-matches"));
            final ConsumerRecord<String, MatchResult>[] recordHolder = new ConsumerRecord[1];

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(500));
                if (!records.isEmpty()) {
                    recordHolder[0] = records.iterator().next();
                }
                assertThat(recordHolder[0]).withFailMessage("No Kafka record received within timeout").isNotNull();
            });

            MatchResult consumed = recordHolder[0].value();
            assertThat(consumed.getId()).isEqualTo("match-1");
            assertThat(consumed.getTeamA()).isEqualTo("TeamA");
            assertThat(consumed.getTeamB()).isEqualTo("TeamB");
            assertThat(consumed.getTeamAScore()).isEqualTo(21);
            assertThat(consumed.getTeamBScore()).isEqualTo(19);
            assertThat(consumed.getWinner()).isEqualTo("TeamA");
            assertThat(consumed.getGameNumber()).isEqualTo(1);
        }
    }
}
