package com.workoutsmart.exercise.dto;

import jakarta.validation.constraints.NotBlank;

/** Tạo bài tập cá nhân — FR-001/FR-002 (011). Media là URL đã upload (nếu có). */
public record CreateCustomExerciseRequest(
        @NotBlank String name,
        @NotBlank String muscleGroup,
        @NotBlank String equipment,
        @NotBlank String category,
        String bodyPart,
        String target,
        String image,
        String gifUrl,
        String instructions) {
}
