package com.workoutsmart.nutrition.repository;

import com.workoutsmart.nutrition.entity.MealLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByUserIdAndLogDateOrderByMealNumberAsc(Long userId, LocalDate logDate);

    List<MealLog> findByUserIdAndLogDateBetween(Long userId, LocalDate from, LocalDate to);

    Optional<MealLog> findByUserIdAndLogDateAndMealNumber(Long userId, LocalDate logDate, int mealNumber);

    List<MealLog> findByLogDateBefore(LocalDate date);
}
