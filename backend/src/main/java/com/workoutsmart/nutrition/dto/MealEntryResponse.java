package com.workoutsmart.nutrition.dto;

import java.math.BigDecimal;

public record MealEntryResponse(
        Long id,
        Long foodItemId,
        String foodName,
        BigDecimal portionGrams,
        BigDecimal totalCalories,
        BigDecimal totalProtein,
        BigDecimal totalCarb,
        BigDecimal totalFat) {
}
