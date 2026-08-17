package com.workoutsmart.profile.repository;

import com.workoutsmart.profile.entity.WorkoutSession;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    Page<WorkoutSession> findByUserIdOrderByStartTimeDesc(Long userId, Pageable pageable);

    List<WorkoutSession> findByUserIdAndStatus(Long userId, String status);

    List<WorkoutSession> findByUserIdAndStatusAndStartTimeBetween(
            Long userId, String status, Instant from, Instant to);

    long countByPlanIdAndStatus(Long planId, String status);
}
