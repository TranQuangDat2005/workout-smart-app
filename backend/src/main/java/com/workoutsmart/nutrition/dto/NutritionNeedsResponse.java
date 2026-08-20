package com.workoutsmart.nutrition.dto;

import java.math.BigDecimal;

/** 019: nhu cầu dinh dưỡng tính từ TDEE — BMR, TDEE, mục tiêu calo, macro, calo mỗi bữa. */
public record NutritionNeedsResponse(
        BigDecimal bmr,
        BigDecimal tdee,
        BigDecimal targetCalories,
        String goalType,
        BigDecimal proteinG,
        BigDecimal carbG,
        BigDecimal fatG,
        BigDecimal perMealCalories,
        int mealsPerDay,
        String sex,
        Integer age,
        BigDecimal heightCm,
        BigDecimal weightKg,
        String activityLevel,
        String calorieGoal) {
}
