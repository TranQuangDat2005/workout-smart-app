package com.workoutsmart.plan.dto;

/** Một bài tập trong một ngày của lộ trình — FR-008, FR-009. */
public record PlanExerciseResponse(
        Long id,
        Long exerciseId,
        String exerciseName,
        int targetSets,
        int targetReps,
        int restTimeSeconds,
        String image,
        String gifUrl) {
}
