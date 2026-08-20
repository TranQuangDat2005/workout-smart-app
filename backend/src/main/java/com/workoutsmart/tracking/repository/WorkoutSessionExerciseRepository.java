package com.workoutsmart.tracking.repository;

import com.workoutsmart.tracking.entity.WorkoutSessionExercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionExerciseRepository extends JpaRepository<WorkoutSessionExercise, Long> {

    List<WorkoutSessionExercise> findBySessionIdOrderBySortOrderAscIdAsc(Long sessionId);

    long deleteBySessionIdIn(List<Long> sessionIds);
}
