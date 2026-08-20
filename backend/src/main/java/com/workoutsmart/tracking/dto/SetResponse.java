package com.workoutsmart.tracking.dto;

import java.math.BigDecimal;

/** Hiệp tập đã lưu — FR-001 (009) + set_type (014) + duration (015). */
public record SetResponse(
        Long id,
        Long sessionId,
        Long exerciseId,
        int setNumber,
        Integer repsCompleted,
        BigDecimal weightUsed,
        Integer restTimeSeconds,
        Long sessionExerciseId,
        String setType,
        Integer durationSeconds) {
}
