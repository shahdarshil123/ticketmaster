package com.ticketmaster.eventservice.catalog.service;

import com.ticketmaster.eventservice.catalog.dto.CreateEventRequest;
import com.ticketmaster.eventservice.catalog.dto.EventResponse;
import com.ticketmaster.eventservice.catalog.dto.PricesResponse;
import com.ticketmaster.eventservice.catalog.dto.SetPricesRequest;
import com.ticketmaster.eventservice.catalog.dto.UpdateEventRequest;
import org.springframework.stereotype.Service;

@Service
public class EventService {

    public EventResponse createEvent(CreateEventRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public EventResponse getEvent(Long eventId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public EventResponse updateEvent(Long eventId, UpdateEventRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public EventResponse publishEvent(Long eventId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public EventResponse cancelEvent(Long eventId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public PricesResponse setPrices(Long eventId, SetPricesRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public PricesResponse getPrices(Long eventId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
