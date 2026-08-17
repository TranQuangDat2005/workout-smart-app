package com.workoutsmart.nutrition.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BodyMetricResponse(
        Long id,
        BigDecimal weightKg,
        BigDecimal bodyFatPct,
        BigDecimal waistCm,
        BigDecimal chestCm,
        BigDecimal armCm,
        BigDecimal deltaWeightKg,
        Instant recordedAt) {
}
