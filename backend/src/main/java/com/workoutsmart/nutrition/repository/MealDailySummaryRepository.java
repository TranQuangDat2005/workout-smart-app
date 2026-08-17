package com.workoutsmart.nutrition.repository;

import com.workoutsmart.nutrition.entity.MealDailySummary;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealDailySummaryRepository extends JpaRepository<MealDailySummary, Long> {

    Optional<MealDailySummary> findByUserIdAndLogDate(Long userId, LocalDate logDate);
}
