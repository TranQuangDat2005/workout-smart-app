package com.workoutsmart.plan.dto;

import java.util.List;

/** Lộ trình tập kèm danh sách ngày và bài tập — FR-002. */
public record WorkoutPlanResponse(
        Long id,
        String name,
        String goalType,
        String fitnessLevel,
        String status,
        List<PlanDayResponse> days) {
}
