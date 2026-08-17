package com.workoutsmart.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Size(max = 500) String avatarUrl,
        @Min(10) @Max(120) Integer age,
        @Positive @Max(300) java.math.BigDecimal heightCm,
        @Pattern(regexp = "weight_loss|muscle_gain|endurance", message = "Mục tiêu không hợp lệ")
        String goalType) {
}
