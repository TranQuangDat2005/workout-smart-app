package com.workoutsmart.nutrition.repository;

import com.workoutsmart.nutrition.entity.MealEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {

    List<MealEntry> findByMealLogId(Long mealLogId);

    void deleteByMealLogId(Long mealLogId);
}
