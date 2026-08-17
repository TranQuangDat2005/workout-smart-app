package com.workoutsmart.tracking.dto;

import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

/** Session offline cần đồng bộ — FR-009 (009). */
public record SyncSessionRequest(
        Long serverSessionId,
        Long planId,
        Instant startTime,
        @NotEmpty List<SyncSetRequest> sets) {
}
