package com.workoutsmart.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Admin ẩn/hiện bài tập — FR-009 (006-admin). */
public record SetExerciseStatusRequest(
        @NotBlank
        @Pattern(regexp = "active|inactive", message = "Trạng thái không hợp lệ")
        String status,

        String reason) {
}
