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
        @Positive @Max(500) java.math.BigDecimal weightKg,
        @Pattern(regexp = "male|female", message = "Giới tính không hợp lệ")
        String sex,
        @Pattern(regexp = "sedentary|light|moderate|active|very_active", message = "Mức vận động không hợp lệ")
        String activityLevel,
        @Pattern(regexp = "maintain|cut_light|cut_fast|bulk_light|bulk_fast|custom", message = "Mức điều chỉnh calo không hợp lệ")
        String calorieGoal,
        @Min(-2000) @Max(2000) Integer customCalorieOffset,
        @Pattern(regexp = "weight_loss|muscle_gain|endurance", message = "Mục tiêu không hợp lệ")
        String goalType) {
}
