package com.ticketmaster.eventservice.common;

import java.time.Instant;

/**
 * Error body returned by every endpoint, e.g. 404 for a missing id or 409 for a state conflict.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp) {
}
