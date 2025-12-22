package com.thomascup.service;

import com.thomascup.model.TrackingEvent;
import com.thomascup.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository);
    }

    @Test
    void addEvent_shouldCallRepositoryAndReturnSavedEvent() {
        // Given
        Instant fixedTime = Instant.parse("2025-01-05T12:30:00Z");
        TrackingEvent event = new TrackingEvent("PN123456789", "ARRIVED_AT_SORTING_FACILITY", fixedTime);
        TrackingEvent savedEvent = new TrackingEvent("PN123456789", "ARRIVED_AT_SORTING_FACILITY", fixedTime);

        when(eventRepository.saveEvent(event)).thenReturn(savedEvent);

        // When
        TrackingEvent result = eventService.addEvent(event);

        // Then
        assertEquals(savedEvent, result);
        verify(eventRepository, times(1)).saveEvent(event);
    }

    @Test
    void addEvent_shouldHandleDuplicateEventsCorrectly() {
        // Given - testing idempotency through service layer
        Instant fixedTime = Instant.parse("2025-01-05T12:30:00Z");
        TrackingEvent event = new TrackingEvent("PN123456789", "STARTED", fixedTime);

        when(eventRepository.saveEvent(event)).thenReturn(event);

        // When
        TrackingEvent firstResult = eventService.addEvent(event);
        TrackingEvent secondResult = eventService.addEvent(event);

        // Then
        assertEquals(event, firstResult);
        assertEquals(event, secondResult);
        verify(eventRepository, times(2)).saveEvent(event);
    }
}
