package com.ticketmaster.eventservice.catalog.dto;

import com.ticketmaster.eventservice.catalog.entity.Category;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Same as {@link CreateEventRequest} except the venue, which cannot change once the event exists.
 */
public record UpdateEventRequest(
        @NotNull Category category,
        @NotBlank String name,
        String description,
        @NotNull @Future Instant startTime) {
}
