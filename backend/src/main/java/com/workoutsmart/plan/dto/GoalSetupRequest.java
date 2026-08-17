package com.workoutsmart.plan.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

/** Thiết lập mục tiêu cá nhân — FR-001. */
public record GoalSetupRequest(
        @NotNull
        @Pattern(regexp = "weight_loss|muscle_gain|endurance", message = "Mục tiêu không hợp lệ")
        String goalType,

        @NotNull
        @Pattern(regexp = "beginner|intermediate|advanced", message = "Trình độ không hợp lệ")
        String fitnessLevel,

        @NotEmpty(message = "Phải chọn ít nhất một dụng cụ")
        List<String> equipment,

        LocalDate effectiveDate) {
}
