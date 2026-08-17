package com.workoutsmart.tracking.dto;

import java.time.Instant;

/** Bắt đầu buổi tập — FR-006 (009). */
public record StartSessionRequest(
        Long planId,
        Instant startTime) {
}
