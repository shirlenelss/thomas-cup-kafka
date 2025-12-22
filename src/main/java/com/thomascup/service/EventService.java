package com.thomascup.service;

import com.thomascup.model.TrackingEvent;
import com.thomascup.repository.EventRepository;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public TrackingEvent addEvent(TrackingEvent event) {
        return eventRepository.saveEvent(event);
    }
}
