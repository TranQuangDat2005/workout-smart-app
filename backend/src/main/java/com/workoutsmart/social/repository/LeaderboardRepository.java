package com.workoutsmart.social.repository;

import com.workoutsmart.social.entity.LeaderboardEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    Optional<LeaderboardEntry> findByUserId(Long userId);
}
