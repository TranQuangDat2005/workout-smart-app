package com.workoutsmart.social.repository;

import com.workoutsmart.social.entity.LeaderboardEntry;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    Optional<LeaderboardEntry> findByUserId(Long userId);

    /** Top 100 theo tie-break FR-007: streak DESC, streakStartWeek ASC, userId ASC. */
    List<LeaderboardEntry> findTop100ByOrderByCurrentStreakWeeksDescStreakStartWeekAscUserIdAsc();

    /**
     * Tính rank của user bằng cách đếm số entry có streak cao hơn,
     * hoặc streak bằng nhưng start sớm hơn, hoặc start bằng nhưng id nhỏ hơn.
     */
    @Query("SELECT COUNT(e) + 1 FROM LeaderboardEntry e WHERE " +
           "e.currentStreakWeeks > :streak OR " +
           "(e.currentStreakWeeks = :streak AND e.streakStartWeek < :startWeek) OR " +
           "(e.currentStreakWeeks = :streak AND e.streakStartWeek = :startWeek AND e.userId < :userId)")
    int findRankByStats(@Param("streak") int streak,
                        @Param("startWeek") LocalDate startWeek,
                        @Param("userId") Long userId);
}
