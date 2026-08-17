package com.workoutsmart.tracking.dto;

import java.util.List;

/** Kết quả sync — FR-009 (009). */
public record SyncResponse(
        int acceptedSessions,
        int acceptedSets,
        List<Long> rejectedSetIds) {
}
