package com.workoutsmart.nutrition.dto;

import java.math.BigDecimal;

public record FoodResponse(
        Long id,
        String name,
        BigDecimal caloriesPer100g,
        BigDecimal proteinPer100g,
        BigDecimal carbPer100g,
        BigDecimal fatPer100g,
        String source) {
}
