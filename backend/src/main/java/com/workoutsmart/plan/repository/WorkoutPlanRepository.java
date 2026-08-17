package com.workoutsmart.plan.repository;

import com.workoutsmart.plan.entity.WorkoutPlan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    List<WorkoutPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
}
