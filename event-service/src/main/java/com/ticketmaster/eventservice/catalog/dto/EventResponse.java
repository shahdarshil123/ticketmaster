package com.ticketmaster.eventservice.catalog.dto;

import com.ticketmaster.eventservice.catalog.entity.Category;
import com.ticketmaster.eventservice.catalog.entity.EventStatus;

import java.time.Instant;

public record EventResponse(
        Long id,
        String name,
        String description,
        Category category,
        Instant startTime,
        EventStatus status,
        VenueSummary venue) {

    public record VenueSummary(Long id, String name, String city) {
    }
}
