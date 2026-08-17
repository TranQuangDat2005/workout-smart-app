package com.workoutsmart.profile.dto;

import java.time.Instant;
import java.util.List;

public record WorkoutSessionDetailResponse(
        Long id,
        Instant startTime,
        Instant endTime,
        String status,
        int focusInterruptionsCount,
        List<WorkoutSetResponse> sets) {
}
