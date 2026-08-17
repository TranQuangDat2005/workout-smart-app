package com.workoutsmart.admin.dto;

/** Danh sách bài tập cho Admin (gồm cả inactive) — FR-008 (006). */
public record AdminExerciseResponse(
        Long id,
        String name,
        String equipment,
        String muscleGroup,
        String status) {
}
