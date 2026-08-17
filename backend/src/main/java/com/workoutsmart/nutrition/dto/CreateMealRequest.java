package com.workoutsmart.nutrition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CreateMealRequest(
        @NotNull @Min(1) @Max(20) Integer mealNumber,
        @NotNull LocalDate logDate,
        @NotEmpty List<@Valid MealEntryRequest> entries) {
}
