package com.workoutsmart.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateBodyMetricRequest(
        @NotNull @DecimalMin(value = "0.1") BigDecimal weightKg,
        BigDecimal bodyFatPct,
        BigDecimal waistCm,
        BigDecimal chestCm,
        BigDecimal armCm) {
}
