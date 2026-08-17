package com.workoutsmart.tracking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

/** Hiệp tập từ offline queue — FR-009 (009). */
public record SyncSetRequest(
        Long exerciseId,

        @NotNull @Min(1)
        Integer setNumber,

        @Min(0)
        Integer repsCompleted,

        @Min(0)
        BigDecimal weightUsed,

        @Min(0)
        Integer restTimeSeconds,

        Instant clientTimestamp) {
}
