package com.workoutsmart.nutrition.service;

import com.workoutsmart.auth.entity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * TDEE = BMR (Mifflin-St Jeor) × hệ số vận động, rồi điều chỉnh theo goal.
 * Đã chốt trong General Spec + constitution.
 */
public class TdeeCalculator {

    /** Hệ số vận động theo activity_level. */
    public static BigDecimal activityFactor(String activityLevel) {
        if (activityLevel == null) {
            throw new IllegalArgumentException("Thiếu mức vận động");
        }
        return switch (activityLevel) {
            case "sedentary" -> new BigDecimal("1.2");
            case "light" -> new BigDecimal("1.375");
            case "moderate" -> new BigDecimal("1.55");
            case "active" -> new BigDecimal("1.725");
            case "very_active" -> new BigDecimal("1.9");
            default -> throw new IllegalArgumentException("Mức vận động không hợp lệ: " + activityLevel);
        };
    }

    /** BMR Mifflin-St Jeor: nam +5, nữ −161. */
    public static BigDecimal bmr(User user) {
        if (user.getSex() == null || user.getAge() == null || user.getHeightCm() == null
                || user.getWeightKg() == null) {
            throw new IllegalArgumentException("Thiếu thông số cơ thể");
        }
        BigDecimal base = BigDecimal.valueOf(10).multiply(user.getWeightKg())
                .add(new BigDecimal("6.25").multiply(user.getHeightCm()))
                .subtract(BigDecimal.valueOf(5).multiply(BigDecimal.valueOf(user.getAge())));
        BigDecimal offset = switch (user.getSex()) {
            case "male" -> new BigDecimal("5");
            case "female" -> new BigDecimal("-161");
            default -> throw new IllegalArgumentException("Giới tính không hợp lệ");
        };
        return base.add(offset);
    }

    /** Mục tiêu calo theo goal_type: cutting −17%, bulking +12%, endurance giữ nguyên. */
    public static BigDecimal targetCalories(User user) {
        BigDecimal tdee = bmr(user).multiply(activityFactor(user.getActivityLevel()));
        BigDecimal target = switch (user.getGoalType() == null ? "endurance" : user.getGoalType()) {
            case "weight_loss" -> tdee.multiply(new BigDecimal("0.83"));
            case "muscle_gain" -> tdee.multiply(new BigDecimal("1.12"));
            default -> tdee;
        };
        return target.setScale(0, RoundingMode.HALF_UP);
    }
}
