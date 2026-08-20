package com.workoutsmart.plan.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Thay thế danh sách bài một ngày template — FR-001/FR-002 (013). */
public record ReplaceDayExercisesRequest(
        @NotNull @Size(max = 15) List<@Valid PlanExerciseItemRequest> exercises) {
}
