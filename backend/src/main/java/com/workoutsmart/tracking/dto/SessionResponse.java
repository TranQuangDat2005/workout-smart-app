package com.workoutsmart.tracking.dto;

import java.time.Instant;
import java.util.List;

/** Trạng thái buổi tập — FR-010 (009) + snapshot 013 + sets đã ghi (015b). */
public record SessionResponse(
        Long id,
        String status,
        Instant startTime,
        Instant endTime,
        int focusInterruptionsCount,
        Long planId,
        List<SessionExerciseResponse> exercises,
        List<SetResponse> sets) {
}
