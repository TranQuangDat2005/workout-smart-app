package com.workoutsmart.tracking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Batch sync offline — FR-009 (009). */
public record SyncRequest(
        @NotNull List<@Valid SyncSessionRequest> sessions) {
}
