package com.thomascup.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;

public record TrackingEvent(
        @NotBlank(message = "trackingId is mandatory")
        String trackingId,
        @NotBlank(message = "status is mandatory")
        String status,
        @PastOrPresent(message = "timestamp is mandatory")
        Instant timestamp
) {}

