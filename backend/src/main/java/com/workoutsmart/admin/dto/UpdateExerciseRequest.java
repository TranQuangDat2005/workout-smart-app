package com.workoutsmart.admin.dto;

/** Chỉnh sửa bài tập — FR-008 (006). Mọi trường tùy chọn (chỉ cập nhật trường được gửi). */
public record UpdateExerciseRequest(
        String name,
        String category,
        String bodyPart,
        String equipment,
        String target,
        String muscleGroup,
        String image,
        String gifUrl,
        String instructions) {
}
