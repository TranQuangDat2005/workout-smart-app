package com.workoutsmart.tracking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Ghi nhận hiệp tập — FR-001 (009). */
public record RecordSetRequest(
        Long exerciseId,

        @NotNull @Min(1)
        Integer setNumber,

        @Min(0)
        Integer repsCompleted,

        @Min(0)
        BigDecimal weightUsed,

        @Min(0)
        Integer restTimeSeconds) {
}
