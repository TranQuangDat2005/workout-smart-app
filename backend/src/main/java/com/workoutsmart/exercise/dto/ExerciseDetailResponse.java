package com.workoutsmart.exercise.dto;

/** Chi tiết bài tập — FR-004, FR-007 (fallback gifUrl → image do client xử lý). */
public record ExerciseDetailResponse(
        Long id,
        String name,
        String category,
        String bodyPart,
        String equipment,
        String target,
        String muscleGroup,
        String image,
        String gifUrl,
        String instructions,
        String source,
        String measureType) {
}
