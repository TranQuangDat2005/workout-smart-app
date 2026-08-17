package com.workoutsmart.plan.repository;

import com.workoutsmart.plan.entity.WorkoutPlanDay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanDayRepository extends JpaRepository<WorkoutPlanDay, Long> {

    long countByPlanId(Long planId);

    List<WorkoutPlanDay> findByPlanIdOrderByDayOfWeekAsc(Long planId);
}
