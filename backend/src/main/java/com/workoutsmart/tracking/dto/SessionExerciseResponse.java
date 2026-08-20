package com.workoutsmart.tracking.dto;

import com.workoutsmart.plan.dto.SetTargetResponse;
import java.util.List;

/** Snapshot bài trong buổi tập — FR-003 (013) + per-set target (014) + duration (015) + mediaUrl (016). */
public record SessionExerciseResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        int sortOrder,
        int targetSets,
        int targetReps,
        int restTimeSeconds,
        List<SetTargetResponse> sets,
        Integer targetDurationSeconds,
        String measureType,
        String mediaUrl) {
}
