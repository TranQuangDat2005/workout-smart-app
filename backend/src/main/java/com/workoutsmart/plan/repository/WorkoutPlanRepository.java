package com.workoutsmart.plan.repository;

import com.workoutsmart.plan.entity.WorkoutPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    List<WorkoutPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WorkoutPlan> findByUserIdAndStatus(Long userId, String status);

    long countByUserIdAndStatus(Long userId, String status);
}
