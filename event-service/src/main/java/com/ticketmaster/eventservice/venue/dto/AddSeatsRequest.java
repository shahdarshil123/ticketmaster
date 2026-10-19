package com.ticketmaster.eventservice.venue.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * Adds seats row by row; each row gets seats numbered 1..seatsPerRow.
 */
public record AddSeatsRequest(@NotEmpty List<@Valid Row> rows) {

    public record Row(
            @NotBlank String rowLabel,
            @NotNull @Positive Integer seatsPerRow) {
    }
}
