package com.workoutsmart.plan.dto;

import java.util.List;

/** Một ngày tập trong lộ trình — FR-002. */
public record PlanDayResponse(
        Long id,
        int dayOfWeek,
        List<PlanExerciseResponse> exercises) {
}
