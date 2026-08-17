package com.workoutsmart.plan.repository;

import com.workoutsmart.plan.entity.DraftExercise;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftExerciseRepository extends JpaRepository<DraftExercise, Long> {

    List<DraftExercise> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
