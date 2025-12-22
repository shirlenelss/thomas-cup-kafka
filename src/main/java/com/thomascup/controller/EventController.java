package com.thomascup.controller;

import com.thomascup.mapper.FlexibleJsonMapper;
import com.thomascup.model.TrackingEvent;
import com.thomascup.service.EventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class EventController {
    private static final Logger log = LoggerFactory.getLogger(EventController.class);
    @Autowired
    private EventService eventService;
    @Autowired
    private FlexibleJsonMapper jsonMapper;

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody @Valid TrackingEvent event) {
        if (event.timestamp().isAfter(Instant.now())) {
            log.error("Event timestamp out of range");
            return ResponseEntity.badRequest().build();
        }
        if (event.trackingId().isEmpty()) {
            log.error("trackingId is null");
            return ResponseEntity.badRequest().build();
        }
        TrackingEvent saved = eventService.addEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/events/flexible")
    public ResponseEntity<?> createEventFlexible(@RequestBody Map<String, Object> jsonBody) {
        try {
            TrackingEvent event = jsonMapper.mapToRecord(jsonBody, TrackingEvent.class);

            // Business validation only
            if (event.timestamp().isAfter(Instant.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "timestamp must be in the past or present"));
            }

            TrackingEvent saved = eventService.addEvent(event);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process flexible event", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid JSON format"));
        }
    }

}
