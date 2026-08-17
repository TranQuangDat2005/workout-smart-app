package com.workoutsmart.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.workoutsmart.auth.entity.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TdeeCalculatorTest {

    private User user(String sex, Integer age, double height, double weight, String activity, String goal) {
        return User.builder()
                .id(1L)
                .sex(sex)
                .age(age)
                .heightCm(new BigDecimal(height))
                .weightKg(new BigDecimal(weight))
                .activityLevel(activity)
                .goalType(goal)
                .build();
    }

    @Test
    void bmrMaleMifflinStJeor() {
        // Nam 68kg 170cm 35t: BMR ≈ 1573 (theo General Spec ví dụ)
        BigDecimal bmr = TdeeCalculator.bmr(user("male", 35, 170, 68, "moderate", "endurance"));
        assertEquals(1573, bmr.setScale(0, java.math.RoundingMode.HALF_UP).intValue());
    }

    @Test
    void bmrFemaleOffset() {
        User male = user("male", 30, 170, 70, "moderate", "endurance");
        User female = user("female", 30, 170, 70, "moderate", "endurance");
        // nữ = nam − 166 (5 vs −161)
        assertEquals(-166, TdeeCalculator.bmr(female).subtract(TdeeCalculator.bmr(male)).intValue());
    }

    @Test
    void activityFactorMapping() {
        assertEquals(0, new BigDecimal("1.2").compareTo(TdeeCalculator.activityFactor("sedentary")));
        assertEquals(0, new BigDecimal("1.375").compareTo(TdeeCalculator.activityFactor("light")));
        assertEquals(0, new BigDecimal("1.55").compareTo(TdeeCalculator.activityFactor("moderate")));
        assertEquals(0, new BigDecimal("1.725").compareTo(TdeeCalculator.activityFactor("active")));
        assertEquals(0, new BigDecimal("1.9").compareTo(TdeeCalculator.activityFactor("very_active")));
    }

    @Test
    void targetCuttingReduces17Percent() {
        User u = user("male", 35, 170, 68, "moderate", "weight_loss");
        BigDecimal tdee = TdeeCalculator.bmr(u).multiply(TdeeCalculator.activityFactor("moderate"));
        BigDecimal target = TdeeCalculator.targetCalories(u);
        assertEquals(0, tdee.multiply(new BigDecimal("0.83")).setScale(0, java.math.RoundingMode.HALF_UP)
                .compareTo(target));
    }

    @Test
    void targetBulkingIncreases12Percent() {
        User u = user("male", 35, 170, 68, "moderate", "muscle_gain");
        BigDecimal tdee = TdeeCalculator.bmr(u).multiply(TdeeCalculator.activityFactor("moderate"));
        BigDecimal target = TdeeCalculator.targetCalories(u);
        assertEquals(0, tdee.multiply(new BigDecimal("1.12")).setScale(0, java.math.RoundingMode.HALF_UP)
                .compareTo(target));
    }

    @Test
    void targetEnduranceKeepsTdee() {
        User u = user("male", 35, 170, 68, "moderate", "endurance");
        BigDecimal tdee = TdeeCalculator.bmr(u).multiply(TdeeCalculator.activityFactor("moderate"));
        assertEquals(0, tdee.setScale(0, java.math.RoundingMode.HALF_UP)
                .compareTo(TdeeCalculator.targetCalories(u)));
    }

    @Test
    void missingBodyDataThrows() {
        User u = user("male", null, 170, 68, "moderate", "endurance");
        assertThrows(IllegalArgumentException.class, () -> TdeeCalculator.bmr(u));
    }

    @Test
    void invalidActivityThrows() {
        assertThrows(IllegalArgumentException.class, () -> TdeeCalculator.activityFactor("super_active"));
    }

    @Test
    void missingActivityThrows() {
        assertThrows(IllegalArgumentException.class, () -> TdeeCalculator.activityFactor(null));
    }

    @Test
    void invalidSexThrows() {
        User u = user("robot", 30, 170, 70, "moderate", "endurance");
        assertThrows(IllegalArgumentException.class, () -> TdeeCalculator.bmr(u));
    }

    @Test
    void nullGoalDefaultsToEndurance() {
        User u = user("male", 35, 170, 68, "moderate", null);
        BigDecimal tdee = TdeeCalculator.bmr(u).multiply(TdeeCalculator.activityFactor("moderate"));
        assertEquals(0, tdee.setScale(0, java.math.RoundingMode.HALF_UP)
                .compareTo(TdeeCalculator.targetCalories(u)));
    }
}
