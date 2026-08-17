package com.workoutsmart.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateFoodRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal caloriesPer100g,
        @NotNull @DecimalMin(value = "0.0") BigDecimal proteinPer100g,
        @NotNull @DecimalMin(value = "0.0") BigDecimal carbPer100g,
        @NotNull @DecimalMin(value = "0.0") BigDecimal fatPer100g) {
}
