package com.workoutsmart.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MealEntryRequest(
        @NotNull Long foodItemId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal portionGrams) {
}
