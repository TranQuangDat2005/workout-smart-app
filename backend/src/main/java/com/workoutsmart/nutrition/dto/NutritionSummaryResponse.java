package com.workoutsmart.nutrition.dto;

import java.math.BigDecimal;

public record NutritionSummaryResponse(
        java.time.LocalDate date,
        BigDecimal totalCalories,
        BigDecimal totalProtein,
        BigDecimal totalCarb,
        BigDecimal totalFat,
        BigDecimal targetCalories,
        BigDecimal deficitOrSurplus,
        String status,
        BigDecimal targetProtein,
        BigDecimal targetCarb,
        BigDecimal targetFat) {
}
