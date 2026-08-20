package com.workoutsmart.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** Một bài trong payload sửa ngày template — FR-001 (013) + per-set target (014) + duration (015). */
public record PlanExerciseItemRequest(
        @NotNull Long exerciseId,
        @Min(1) @Max(20) int targetSets,
        @Min(0) @Max(100) int targetReps,
        @Min(0) @Max(600) int restTimeSeconds,
        List<@Valid SetTargetRequest> sets,
        @Min(0) Integer targetDurationSeconds,
        @Pattern(regexp = "reps_weight|duration") String measureType) {
}
