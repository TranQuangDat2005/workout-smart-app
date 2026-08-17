package com.workoutsmart.admin.dto;

/** Báo cáo kết quả import — FR-016 (006). */
public record ExerciseImportResponse(
        int inserted,
        int updated,
        int skipped) {
}
