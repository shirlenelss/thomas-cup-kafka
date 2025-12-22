package com.thomascup.repository;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EventRepositoryTest {

    @Test
    void saveEvent() {
        EventRepository eventRepository = new EventRepository();
        var event = new com.thomascup.model.TrackingEvent("123", "STARTED", java.time.Instant.now());
        var savedEvent = eventRepository.saveEvent(event);
        assertEquals(event, savedEvent);
    }

    @Test
    void idempotentSaveEvent() {
        EventRepository eventRepository = new EventRepository();
        Instant now = Instant.now();
        var event = new com.thomascup.model.TrackingEvent("123", "STARTED", now);
        var savedEvent = eventRepository.saveEvent(event);
        assertEquals(event, savedEvent);
        var duplicatedEvent = new com.thomascup.model.TrackingEvent("123", "STARTED", now);
        var savedDuplicatedEvent = eventRepository.saveEvent(duplicatedEvent);
        assertEquals(duplicatedEvent, savedDuplicatedEvent);
        eventRepository.getEvents("123").forEach(e -> System.out.println(e));
        assertEquals(1, eventRepository.getEvents("123").size());
    }
}