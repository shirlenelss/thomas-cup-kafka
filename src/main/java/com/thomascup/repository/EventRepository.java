package com.thomascup.repository;

import com.thomascup.model.TrackingEvent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class EventRepository {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<TrackingEvent>> events = new ConcurrentHashMap<>();

    public TrackingEvent saveEvent(TrackingEvent event) {
        if (events.containsKey(event.trackingId())) {
            var existingEvents = events.get(event.trackingId());
            for (TrackingEvent existingEvent : existingEvents) {
                if (Objects.equals(existingEvent, event)) {
                    // Duplicate event found, do not add
                    return existingEvent;
                }
            }
        }
        events.computeIfAbsent(event.trackingId(),
                        id -> new CopyOnWriteArrayList<>())
                .add(event);
        return event;
    }

    public List<TrackingEvent> getEvents(String trackingId) {
        return events.getOrDefault(trackingId, new CopyOnWriteArrayList<>());
    }
}
