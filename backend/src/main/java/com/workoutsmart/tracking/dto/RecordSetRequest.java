package com.workoutsmart.tracking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/** Ghi nhận hiệp tập — FR-001 (009) + set_type (014) + duration (015). */
public record RecordSetRequest(
        Long exerciseId,

        @NotNull @Min(1)
        Integer setNumber,

        @Min(0)
        Integer repsCompleted,

        @Min(0)
        BigDecimal weightUsed,

        @Min(0)
        Integer restTimeSeconds,

        Long sessionExerciseId,

        @Pattern(regexp = "normal|warm_up|drop_set")
        String setType,

        @Min(0)
        Integer durationSeconds) {
}
