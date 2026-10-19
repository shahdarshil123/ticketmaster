package com.ticketmaster.eventservice.venue.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSectionRequest(@NotBlank String name) {
}
