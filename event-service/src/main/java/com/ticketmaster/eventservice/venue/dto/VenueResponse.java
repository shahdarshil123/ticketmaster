package com.ticketmaster.eventservice.venue.dto;

import java.util.List;

public record VenueResponse(
        Long id,
        String name,
        String address,
        String city,
        Double latitude,
        Double longitude,
        List<SectionSummary> sections) {

    public record SectionSummary(Long id, String name) {
    }
}
