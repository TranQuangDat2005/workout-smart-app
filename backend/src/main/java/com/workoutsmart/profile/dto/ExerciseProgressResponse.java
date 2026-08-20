package com.workoutsmart.profile.dto;

import java.math.BigDecimal;

/** Chỉ số tiến bộ một bài tập trong khoảng thời gian (tạ/rep đầu kỳ vs hiện tại). */
public record ExerciseProgressResponse(
        Long exerciseId,
        String exerciseName,
        BigDecimal firstWeight,
        BigDecimal lastWeight,
        BigDecimal weightDelta,
        Integer firstReps,
        Integer lastReps,
        Integer repsDelta) {
}
