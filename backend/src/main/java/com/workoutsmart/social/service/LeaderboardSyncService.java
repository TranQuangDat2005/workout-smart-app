package com.workoutsmart.social.service;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.social.entity.LeaderboardEntry;
import com.workoutsmart.social.repository.LeaderboardRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồng bộ leaderboard: cập nhật incremental khi user hoàn thành buổi tập,
 * và chạy batch mỗi 5 phút để gán rank (FR-009).
 */
@Service
public class LeaderboardSyncService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardSyncService.class);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final LeaderboardRepository leaderboardRepository;
    private final UserRepository userRepository;
    private final WorkoutSessionRepository sessionRepository;

    private volatile Instant lastSyncTime = Instant.now().minus(5, ChronoUnit.MINUTES);

    public LeaderboardSyncService(LeaderboardRepository leaderboardRepository,
                                   UserRepository userRepository,
                                   WorkoutSessionRepository sessionRepository) {
        this.leaderboardRepository = leaderboardRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Cập nhật entry leaderboard cho 1 user — gọi từ ProfileService.completeSession.
     */
    @Transactional
    public void updateEntry(Long userId) {
        List<Instant> starts = sessionRepository.findByUserIdAndStatus(userId, "completed")
                .stream()
                .map(WorkoutSession::getStartTime)
                .toList();
        StreakCalculator.StreakResult streak = new StreakCalculator().calculate(starts, ZONE);

        LeaderboardEntry entry = leaderboardRepository.findByUserId(userId).orElseGet(() ->
                LeaderboardEntry.builder().userId(userId).build());
        entry.setCurrentStreakWeeks(streak.currentStreakWeeks());
        entry.setLongestStreakWeeks(streak.longestStreakWeeks());
        entry.setStreakStartWeek(streak.streakStartWeek());
        leaderboardRepository.save(entry);
    }

    /**
     * Job batch mỗi 5 phút: tìm user có session mới từ lần chạy trước,
     * cập nhật entry, rồi gán rank cho toàn bộ (FR-009).
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    @Transactional
    public void syncBatch() {
        Instant since = lastSyncTime;
        lastSyncTime = Instant.now();

        // Tìm user ACTIVE có session completed từ lần sync trước
        List<Long> activeUserIds = userRepository.findAll().stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .map(User::getId)
                .toList();

        for (Long userId : activeUserIds) {
            boolean hasNewSession = sessionRepository.findByUserIdOrderByStartTimeDesc(userId,
                    org.springframework.data.domain.PageRequest.of(0, 1)).stream()
                    .anyMatch(s -> "completed".equals(s.getStatus())
                            && s.getStartTime() != null
                            && s.getStartTime().isAfter(since));
            if (hasNewSession) {
                try {
                    updateEntry(userId);
                } catch (Exception e) {
                    log.warn("Không thể cập nhật leaderboard cho user {}", userId, e);
                }
            }
        }

        assignRanks();
    }

    /**
     * Gán rank: sort theo currentStreakWeeks DESC, streakStartWeek ASC, userId ASC.
     */
    private void assignRanks() {
        List<LeaderboardEntry> all = leaderboardRepository.findAll();
        all.sort(Comparator
                .comparingInt(LeaderboardEntry::getCurrentStreakWeeks).reversed()
                .thenComparing(Comparator.comparing(LeaderboardEntry::getStreakStartWeek,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .thenComparingLong(LeaderboardEntry::getUserId));

        int rank = 1;
        for (LeaderboardEntry entry : all) {
            entry.setRank(rank++);
        }
        leaderboardRepository.saveAll(all);
    }
}
