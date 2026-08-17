package com.workoutsmart.plan.repository;

import com.workoutsmart.plan.entity.WorkoutPlanExercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanExerciseRepository extends JpaRepository<WorkoutPlanExercise, Long> {

    List<WorkoutPlanExercise> findByDayIdOrderByIdAsc(Long dayId);
}
