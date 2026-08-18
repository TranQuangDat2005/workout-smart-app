package com.workoutsmart.admin.dto;

/** Chi tiết bài tập cho Admin (đầy đủ trường + status) — phục vụ form sửa (FR-008 006). */
public record AdminExerciseDetailResponse(
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
        String status) {
}
