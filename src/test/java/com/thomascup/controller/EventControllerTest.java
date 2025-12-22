package com.thomascup.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomascup.mapper.FlexibleJsonMapper;
import com.thomascup.model.TrackingEvent;
import com.thomascup.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(FlexibleJsonMapper.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private EventService eventService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createEvent_validEvent_returns201Created() throws Exception {
        // Given
        Instant pastTime = Instant.parse("2025-01-05T12:30:00Z");
        TrackingEvent event = new TrackingEvent("PN123456789", "ARRIVED_AT_SORTING_FACILITY", pastTime);

        when(eventService.addEvent(any(TrackingEvent.class))).thenReturn(event);

        // When & Then
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingId").value("PN123456789"))
                .andExpect(jsonPath("$.status").value("ARRIVED_AT_SORTING_FACILITY"))
                .andExpect(jsonPath("$.timestamp").value("2025-01-05T12:30:00Z"));
    }


    @Test
    void createEvent_futureTimestamp_returns400BadRequest() throws Exception {
        // Given
        Instant futureTime = Instant.now().plusSeconds(3600); // 1 hour in future
        TrackingEvent event = new TrackingEvent("PN123456789", "STARTED", futureTime);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
        }

    @Test
    void createEvent_emptyTrackingId_returns400BadRequest() throws Exception {
        // Given - testing validation
        Instant pastTime = Instant.parse("2025-01-05T12:30:00Z");
        TrackingEvent event = new TrackingEvent("", "STARTED", pastTime);

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_presentTimestamp_returns201Created() throws Exception {
        // Given - edge case for present time
        Instant now = Instant.now();
        TrackingEvent event = new TrackingEvent("PN123456789", "PROCESSING", now);

        when(eventService.addEvent(any(TrackingEvent.class))).thenReturn(event);

        // When & Then
        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated());
    }

    @Test
    void createEventFlexible() throws Exception {
        // This test can be implemented similarly to the above tests,
        // focusing on the /events/flexible endpoint and its specific behavior.
        // Given
        Instant pastTime = Instant.parse("2025-01-05T12:30:00Z");
        TrackingEvent expectedEvent = new TrackingEvent("PN123456789", "STARTED", pastTime);

        // Add the missing mock for eventService
        when(eventService.addEvent(any(TrackingEvent.class))).thenReturn(expectedEvent);

        String flexibleJson = """
        {
            "trackingId": "PN123456789",
            "status": "STARTED",
            "timestamp": "2025-01-05T12:30:00Z",
            "extraField": "ignored",
            "anotherField": "12345"
        }
        """;
        // When & Then
        mockMvc.perform(post("/events/flexible")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(flexibleJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingId").value(expectedEvent.trackingId()))
                .andExpect(jsonPath("$.status").value(expectedEvent.status()))
                .andExpect(jsonPath("$.timestamp").value(pastTime.toString()));
    }

    @Test
    void createEventFlexible_invalidData_return400BadRequest() throws Exception {
        // Given - Invalid JSON with missing trackingId and syntax error fixed
        String invalidJson = """
        {
            "status": "STARTED",
            "timestamp": "2025-01-05T12:30:00Z"
        }
        """;
        // When & Then
        mockMvc.perform(post("/events/flexible")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("trackingId is mandatory"));;

    }
}