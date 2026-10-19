package com.ticketmaster.eventservice.venue.dto;

import java.util.List;

public record VenueLayoutResponse(Long venueId, List<SectionLayout> sections) {

    public record SectionLayout(Long id, String name, List<SeatSummary> seats) {
    }

    public record SeatSummary(Long id, String rowLabel, Integer seatNumber) {
    }
}
