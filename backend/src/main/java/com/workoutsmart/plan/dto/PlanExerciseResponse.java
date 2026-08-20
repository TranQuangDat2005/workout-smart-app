package com.workoutsmart.plan.dto;

import java.util.List;

/** Một bài tập trong một ngày của lộ trình — FR-008, FR-009 + per-set target (014) + duration (015). */
public record PlanExerciseResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        int targetSets,
        int targetReps,
        int restTimeSeconds,
        String image,
        String gifUrl,
        List<SetTargetResponse> sets,
        Integer targetDurationSeconds,
        String measureType) {
}
