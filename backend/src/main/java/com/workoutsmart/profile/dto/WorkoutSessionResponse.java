package com.workoutsmart.profile.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkoutSessionResponse(
        Long id,
        Instant startTime,
        Instant endTime,
        String status,
        int focusInterruptionsCount,
        long totalSets,
        BigDecimal totalVolumeKg) {
}
