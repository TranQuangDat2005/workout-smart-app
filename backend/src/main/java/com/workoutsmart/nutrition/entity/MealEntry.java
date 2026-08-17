package com.workoutsmart.nutrition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "meal_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MealEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_log_id", nullable = false)
    private Long mealLogId;

    @Column(name = "food_item_id", nullable = false)
    private Long foodItemId;

    @Column(name = "portion_grams", nullable = false)
    private BigDecimal portionGrams;

    @Column(name = "total_calories", nullable = false)
    private BigDecimal totalCalories;

    @Column(name = "total_protein", nullable = false)
    private BigDecimal totalProtein;

    @Column(name = "total_carb", nullable = false)
    private BigDecimal totalCarb;

    @Column(name = "total_fat", nullable = false)
    private BigDecimal totalFat;
}
