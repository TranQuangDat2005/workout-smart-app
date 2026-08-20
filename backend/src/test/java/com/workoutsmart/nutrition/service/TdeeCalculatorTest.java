package com.workoutsmart.nutrition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.workoutsmart.auth.entity.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TdeeCalculatorTest {

    private User user(String sex, Integer age, double height, double weight, String activity, String calorieGoal) {
        return User.builder()
                .id(1L)
                .sex(sex)
                .age(age)
                .heightCm(new BigDecimal(height))
                .weightKg(new BigDecimal(weight))
                .activityLevel(activity)
                .calorieGoal(calorieGoal)
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
    void targetMaintainKeepsTdee() {
        User u = user("male", 35, 170, 68, "moderate", "maintain");
        // TDEE = 1572.5 × 1.55 = 2437.375 → 2437; maintain giữ nguyên.
        assertEquals(0, new BigDecimal("2437").compareTo(TdeeCalculator.targetCalories(u)));
    }

    @Test
    void targetCutLightSubtracts300() {
        User u = user("male", 35, 170, 68, "moderate", "cut_light");
        assertEquals(0, new BigDecimal("2137").compareTo(TdeeCalculator.targetCalories(u)));
    }

    @Test
    void targetCutFastSubtracts500() {
        User u = user("male", 35, 170, 68, "moderate", "cut_fast");
        assertEquals(0, new BigDecimal("1937").compareTo(TdeeCalculator.targetCalories(u)));
    }

    @Test
    void targetBulkLightAdds300() {
        User u = user("male", 35, 170, 68, "moderate", "bulk_light");
        assertEquals(0, new BigDecimal("2737").compareTo(TdeeCalculator.targetCalories(u)));
    }

    @Test
    void targetBulkFastAdds500() {
        User u = user("male", 35, 170, 68, "moderate", "bulk_fast");
        assertEquals(0, new BigDecimal("2937").compareTo(TdeeCalculator.targetCalories(u)));
    }

    @Test
    void nullCalorieGoalDefaultsToMaintain() {
        User u = user("male", 35, 170, 68, "moderate", null);
        assertEquals(0, new BigDecimal("2437").compareTo(TdeeCalculator.targetCalories(u)));
    }

    @Test
    void targetCustomUsesOffset() {
        User u = user("male", 35, 170, 68, "moderate", "custom");
        u.setCustomCalorieOffset(-350);
        assertEquals(0, new BigDecimal("2087").compareTo(TdeeCalculator.targetCalories(u)));
        u.setCustomCalorieOffset(250);
        assertEquals(0, new BigDecimal("2687").compareTo(TdeeCalculator.targetCalories(u)));
        // custom mà chưa nhập offset → giữ nguyên TDEE.
        u.setCustomCalorieOffset(null);
        assertEquals(0, new BigDecimal("2437").compareTo(TdeeCalculator.targetCalories(u)));
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
    void nullCalorieGoalDefaultsToMaintain2() {
        User u = User.builder().id(1L).sex("male").age(35).heightCm(new BigDecimal("170"))
                .weightKg(new BigDecimal("68")).activityLevel("moderate").build();
        // calorieGoal không set → builder default "maintain" → bằng TDEE.
        assertEquals(0, new BigDecimal("2437").compareTo(TdeeCalculator.targetCalories(u)));
    }
}
