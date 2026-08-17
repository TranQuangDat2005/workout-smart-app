package com.workoutsmart.tracking.dto;

import java.math.BigDecimal;

/** Hiệp tập đã lưu — FR-001 (009). */
public record SetResponse(
        Long id,
        Long sessionId,
        Long exerciseId,
        int setNumber,
        Integer repsCompleted,
        BigDecimal weightUsed,
        Integer restTimeSeconds) {
}
