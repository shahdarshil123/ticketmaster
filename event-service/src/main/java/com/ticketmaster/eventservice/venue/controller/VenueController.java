package com.ticketmaster.eventservice.venue.controller;

import com.ticketmaster.eventservice.venue.service.VenueService;
import com.ticketmaster.eventservice.venue.dto.AddSeatsRequest;
import com.ticketmaster.eventservice.venue.dto.AddSeatsResponse;
import com.ticketmaster.eventservice.venue.dto.CreateSectionRequest;
import com.ticketmaster.eventservice.venue.dto.CreateVenueRequest;
import com.ticketmaster.eventservice.venue.dto.SectionResponse;
import com.ticketmaster.eventservice.venue.dto.VenueLayoutResponse;
import com.ticketmaster.eventservice.venue.dto.VenueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Venues and their seating layout. Routing only; all logic lives in {@link VenueService}.
 */
@RestController
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping("/api/venues")
    public ResponseEntity<VenueResponse> createVenue(@Valid @RequestBody CreateVenueRequest request) {
        VenueResponse venue = venueService.createVenue(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(venue.id())
                .toUri();
        return ResponseEntity.created(location).body(venue);
    }

    @GetMapping("/api/venues/{venueId}")
    public VenueResponse getVenue(@PathVariable Long venueId) {
        return venueService.getVenue(venueId);
    }

    @PostMapping("/api/venues/{venueId}/sections")
    public ResponseEntity<SectionResponse> createSection(@PathVariable Long venueId,
                                                         @Valid @RequestBody CreateSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.createSection(venueId, request));
    }

    @PostMapping("/api/sections/{sectionId}/seats")
    public ResponseEntity<AddSeatsResponse> addSeats(@PathVariable Long sectionId,
                                                     @Valid @RequestBody AddSeatsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.addSeats(sectionId, request));
    }

    @GetMapping("/api/venues/{venueId}/layout")
    public VenueLayoutResponse getLayout(@PathVariable Long venueId) {
        return venueService.getLayout(venueId);
    }
}
