package com.workoutsmart.admin.dto;

import jakarta.validation.constraints.NotBlank;

/** Thêm mới bài tập — FR-007 (006). Không upload media, chỉ nhận path có sẵn. */
public record CreateExerciseRequest(
        @NotBlank String name,
        String category,
        String bodyPart,
        @NotBlank String equipment,
        String target,
        @NotBlank String muscleGroup,
        String image,
        String gifUrl,
        String instructions) {
}
