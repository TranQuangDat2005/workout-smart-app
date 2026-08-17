package com.workoutsmart.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTO response của dashboard thống kê (UC-17). */
public record StatsDashboardResponse(
        List<WeightPoint> weight,
        List<VolumePoint> volume,
        StreakStats streak,
        PlanCompletion planCompletion,
        List<CaloriePoint> calories) {

    public record WeightPoint(LocalDate date, BigDecimal weightKg) {
    }

    public record VolumePoint(LocalDate weekStart, BigDecimal totalKg) {
    }

    public record StreakStats(int currentStreakWeeks, int longestStreakWeeks) {
    }

    public record PlanCompletion(long completedSessions, long plannedDays, BigDecimal completionPct) {
    }

    public record CaloriePoint(LocalDate date, BigDecimal caloriesIn, BigDecimal caloriesBurned) {
    }
}
