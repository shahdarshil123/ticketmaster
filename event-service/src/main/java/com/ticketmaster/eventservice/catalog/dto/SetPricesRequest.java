package com.ticketmaster.eventservice.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Replaces all section prices of an event.
 */
public record SetPricesRequest(@NotEmpty List<@Valid Entry> prices) {

    public record Entry(
            @NotNull Long sectionId,
            @NotNull @Positive BigDecimal price) {
    }
}
