package com.thomascup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomascup.model.MatchResult;
import com.thomascup.service.MatchResultProducer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@SuppressWarnings("deprecation")
public class MatchResultControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchResultProducer matchResultProducer;
    
    @MockBean
    private KafkaTemplate<String, MatchResult> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSendMatchResult() throws Exception {
        MatchResult result = new MatchResult(
            "match-1",
            "TeamA",
            "TeamB",
            21,
            19,
            "TeamA",
            LocalDateTime.now(),
            1
        );
        String json = objectMapper.writeValueAsString(result);
        mockMvc.perform(post("/api/match-results")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("Match result sent to Kafka"));
        Mockito.verify(matchResultProducer).sendMatchResult(Mockito.any(MatchResult.class));
    }
}
