package com.workoutsmart.profile.repository;

import com.workoutsmart.profile.entity.WorkoutSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {

    List<WorkoutSet> findBySessionIdOrderBySetNumberAsc(Long sessionId);

    List<WorkoutSet> findBySessionIdIn(List<Long> sessionIds);
}
