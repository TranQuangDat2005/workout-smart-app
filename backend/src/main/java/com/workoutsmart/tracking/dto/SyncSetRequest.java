package com.workoutsmart.tracking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;

/** Hiệp tập từ offline queue — FR-009 (009) + set_type (014) + duration (015). */
public record SyncSetRequest(
        Long exerciseId,

        Long sessionExerciseId,

        @NotNull @Min(1)
        Integer setNumber,

        @Min(0)
        Integer repsCompleted,

        @Min(0)
        BigDecimal weightUsed,

        @Min(0)
        Integer restTimeSeconds,

        Instant clientTimestamp,

        @Pattern(regexp = "normal|warm_up|drop_set")
        String setType,

        @Min(0)
        Integer durationSeconds) {
}
