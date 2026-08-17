package com.workoutsmart.exercise.dto;

import java.util.List;

/** Kết quả tìm kiếm bài tập phân trang — FR-003, FR-006. */
public record ExerciseSearchResponse(
        List<ExerciseDetailResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size) {
}
