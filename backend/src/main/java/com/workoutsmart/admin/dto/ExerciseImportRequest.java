package com.workoutsmart.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Import hàng loạt bài tập — FR-014/015/016 (006). */
public record ExerciseImportRequest(
        @NotEmpty(message = "Danh sách bài tập không được rỗng")
        List<@Valid CreateExerciseRequest> items) {
}
