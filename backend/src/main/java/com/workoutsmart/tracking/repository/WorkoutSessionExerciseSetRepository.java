package com.workoutsmart.tracking.repository;

import com.workoutsmart.tracking.entity.WorkoutSessionExerciseSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionExerciseSetRepository extends JpaRepository<WorkoutSessionExerciseSet, Long> {

    List<WorkoutSessionExerciseSet> findBySessionExerciseIdOrderBySetNumberAsc(Long sessionExerciseId);

    List<WorkoutSessionExerciseSet> findBySessionExerciseIdIn(List<Long> sessionExerciseIds);

    Optional<WorkoutSessionExerciseSet> findBySessionExerciseIdAndSetNumber(Long sessionExerciseId, int setNumber);
}
