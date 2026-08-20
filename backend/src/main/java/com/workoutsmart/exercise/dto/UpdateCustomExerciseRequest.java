package com.workoutsmart.exercise.dto;

/** Sửa bài tập cá nhân — FR-006 (011). Mọi trường tùy chọn (chỉ cập nhật trường được gửi). */
public record UpdateCustomExerciseRequest(
        String name,
        String muscleGroup,
        String equipment,
        String category,
        String bodyPart,
        String target,
        String image,
        String gifUrl,
        String instructions) {
}
