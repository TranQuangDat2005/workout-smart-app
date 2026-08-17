package com.workoutsmart.nutrition.dto;

import java.math.BigDecimal;
import java.util.List;

public record MealResponse(
        Long id,
        int mealNumber,
        java.time.LocalDate logDate,
        List<MealEntryResponse> entries,
        BigDecimal totalCalories,
        BigDecimal totalProtein,
        BigDecimal totalCarb,
        BigDecimal totalFat) {
}
