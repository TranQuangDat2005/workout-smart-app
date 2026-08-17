package com.workoutsmart.profile.repository;

import com.workoutsmart.profile.entity.WorkoutSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    Page<WorkoutSession> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);
}
