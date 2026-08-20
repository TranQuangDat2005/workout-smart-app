package com.workoutsmart.plan.dto;

import java.math.BigDecimal;

/** Target một hiệp đã lưu — FR-003 (014) + duration (015). */
public record SetTargetResponse(
        int setNumber,
        int targetReps,
        BigDecimal targetWeight,
        String setType,
        Integer targetDurationSeconds) {
}
