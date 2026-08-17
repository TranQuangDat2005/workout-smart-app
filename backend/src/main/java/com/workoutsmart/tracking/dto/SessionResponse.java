package com.workoutsmart.tracking.dto;

import java.time.Instant;

/** Trạng thái buổi tập — FR-010 (009). */
public record SessionResponse(
        Long id,
        String status,
        Instant startTime,
        Instant endTime,
        int focusInterruptionsCount) {
}
