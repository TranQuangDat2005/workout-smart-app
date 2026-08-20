package com.workoutsmart.exercise;

import java.util.Set;

/** Taxonomy kho dữ liệu 012 — khớp exercises-dataset (trừ target muscle). */
public final class ExerciseTaxonomy {

    public static final Set<String> CATEGORIES = Set.of(
            "back", "cardio", "chest", "lower arms", "lower legs", "neck",
            "shoulders", "upper arms", "upper legs", "waist");

    public static final Set<String> EQUIPMENT = Set.of(
            "assisted", "band", "barbell", "body_weight", "bosu_ball", "cable", "dumbbell",
            "elliptical_machine", "ez_barbell", "hammer", "kettlebell", "leverage_machine",
            "medicine_ball", "olympic_barbell", "resistance_band", "roller", "rope",
            "skierg_machine", "sled_machine", "smith_machine", "stability_ball",
            "stationary_bike", "stepmill_machine", "tire", "trap_bar",
            "upper_body_ergometer", "weighted", "wheel_roller");

    public static final Set<String> MUSCLE_GROUPS = Set.of(
            "chest", "back", "shoulders", "arms", "legs", "core");

    private ExerciseTaxonomy() {
    }

    public static boolean isCategory(String value) {
        return value != null && CATEGORIES.contains(value);
    }

    public static boolean isEquipment(String value) {
        return value != null && EQUIPMENT.contains(value);
    }

    public static boolean isMuscleGroup(String value) {
        return value != null && MUSCLE_GROUPS.contains(value);
    }
}
