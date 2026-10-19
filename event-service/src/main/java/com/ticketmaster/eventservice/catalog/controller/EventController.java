package com.ticketmaster.eventservice.catalog.controller;

import com.ticketmaster.eventservice.catalog.service.EventService;
import com.ticketmaster.eventservice.catalog.dto.CreateEventRequest;
import com.ticketmaster.eventservice.catalog.dto.EventResponse;
import com.ticketmaster.eventservice.catalog.dto.PricesResponse;
import com.ticketmaster.eventservice.catalog.dto.SetPricesRequest;
import com.ticketmaster.eventservice.catalog.dto.UpdateEventRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Events, their lifecycle (publish/cancel) and section prices. Routing only; all logic lives in {@link EventService}.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse event = eventService.createEvent(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(event.id())
                .toUri();
        return ResponseEntity.created(location).body(event);
    }

    @GetMapping("/{eventId}")
    public EventResponse getEvent(@PathVariable Long eventId) {
        return eventService.getEvent(eventId);
    }

    @PutMapping("/{eventId}")
    public EventResponse updateEvent(@PathVariable Long eventId, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.updateEvent(eventId, request);
    }

    @PostMapping("/{eventId}/publish")
    public EventResponse publishEvent(@PathVariable Long eventId) {
        return eventService.publishEvent(eventId);
    }

    @PostMapping("/{eventId}/cancel")
    public EventResponse cancelEvent(@PathVariable Long eventId) {
        return eventService.cancelEvent(eventId);
    }

    @PutMapping("/{eventId}/prices")
    public PricesResponse setPrices(@PathVariable Long eventId, @Valid @RequestBody SetPricesRequest request) {
        return eventService.setPrices(eventId, request);
    }

    @GetMapping("/{eventId}/prices")
    public PricesResponse getPrices(@PathVariable Long eventId) {
        return eventService.getPrices(eventId);
    }
}
