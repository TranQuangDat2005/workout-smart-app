package com.workoutsmart.profile.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProfileResponse(
        Long id,
        String email,
        String displayName,
        String avatarUrl,
        Integer age,
        BigDecimal weightKg,
        BigDecimal heightCm,
        String goalType,
        String fitnessLevel,
        String sex,
        String activityLevel,
        String calorieGoal,
        Integer customCalorieOffset,
        String accountStatus,
        boolean emailVerified,
        boolean isPrivate,
        Instant createdAt) {
}
