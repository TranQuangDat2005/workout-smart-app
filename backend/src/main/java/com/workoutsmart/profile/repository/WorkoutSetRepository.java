package com.workoutsmart.profile.repository;

import com.workoutsmart.profile.entity.WorkoutSet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {

    List<WorkoutSet> findBySessionIdOrderBySetNumberAsc(Long sessionId);

    Optional<WorkoutSet> findBySessionIdAndSetNumber(Long sessionId, int setNumber);

    Optional<WorkoutSet> findBySessionIdAndSessionExerciseIdAndSetNumber(
            Long sessionId, Long sessionExerciseId, int setNumber);

    List<WorkoutSet> findBySessionIdIn(List<Long> sessionIds);

    long deleteBySessionIdIn(List<Long> sessionIds);
}
