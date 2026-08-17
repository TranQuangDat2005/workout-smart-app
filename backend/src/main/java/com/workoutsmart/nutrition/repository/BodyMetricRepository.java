package com.workoutsmart.nutrition.repository;

import com.workoutsmart.nutrition.entity.BodyMetric;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BodyMetricRepository extends JpaRepository<BodyMetric, Long> {

    List<BodyMetric> findTop100ByUserIdOrderByRecordedAtDesc(Long userId);

    Optional<BodyMetric> findFirstByUserIdOrderByRecordedAtDesc(Long userId);

    List<BodyMetric> findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long userId, Instant from, Instant to);
}
