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

    /**
     * Mục tiêu calo theo mức điều chỉnh của từng người (019 — owner chốt 2026-08-19):
     * maintain = TDEE giữ nguyên; cut_light = −300; cut_fast = −500;
     * bulk_light = +300; bulk_fast = +500; custom = custom_calorie_offset (kcal/ngày).
     */
    public static BigDecimal targetCalories(User user) {
        BigDecimal tdee = bmr(user).multiply(activityFactor(user.getActivityLevel()))
                .setScale(0, RoundingMode.HALF_UP);
        return tdee.add(calorieOffset(user)).max(BigDecimal.ZERO);
    }

    /** Offset calo theo calorie_goal (custom → dùng custom_calorie_offset). */
    public static BigDecimal calorieOffset(User user) {
        String mode = user.getCalorieGoal() == null ? "maintain" : user.getCalorieGoal();
        if ("custom".equals(mode)) {
            return user.getCustomCalorieOffset() == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(user.getCustomCalorieOffset());
        }
        return switch (mode) {
            case "cut_light" -> new BigDecimal("-300");
            case "cut_fast" -> new BigDecimal("-500");
            case "bulk_light" -> new BigDecimal("300");
            case "bulk_fast" -> new BigDecimal("500");
            default -> BigDecimal.ZERO;
        };
    }

    /** TDEE làm tròn về kcal. */
    public static BigDecimal tdee(User user) {
        return bmr(user).multiply(activityFactor(user.getActivityLevel()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    /** Macro theo mục tiêu calo: Protein 2g/kg cân nặng; Fat 25% calo; Carb = phần còn lại. */
    public record MacroTarget(BigDecimal proteinG, BigDecimal carbG, BigDecimal fatG) {}

    public static MacroTarget macros(User user) {
        BigDecimal target = targetCalories(user);
        BigDecimal proteinG = new BigDecimal("2.0").multiply(user.getWeightKg());
        BigDecimal fatG = target.multiply(new BigDecimal("0.25"))
                .divide(new BigDecimal("9"), 1, RoundingMode.HALF_UP);
        BigDecimal proteinKcal = proteinG.multiply(new BigDecimal("4"));
        BigDecimal fatKcal = fatG.multiply(new BigDecimal("9"));
        BigDecimal carbG = target.subtract(proteinKcal).subtract(fatKcal)
                .divide(new BigDecimal("4"), 1, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO);
        return new MacroTarget(proteinG.setScale(1, RoundingMode.HALF_UP), carbG, fatG);
    }
}
