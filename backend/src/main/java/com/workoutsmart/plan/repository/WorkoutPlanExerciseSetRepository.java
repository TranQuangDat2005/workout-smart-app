package com.workoutsmart.plan.repository;

import com.workoutsmart.plan.entity.WorkoutPlanExerciseSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanExerciseSetRepository extends JpaRepository<WorkoutPlanExerciseSet, Long> {

    List<WorkoutPlanExerciseSet> findByPlanExerciseIdOrderBySetNumberAsc(Long planExerciseId);

    void deleteByPlanExerciseId(Long planExerciseId);
}
