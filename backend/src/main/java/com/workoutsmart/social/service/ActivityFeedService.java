package com.workoutsmart.social.service;

import com.workoutsmart.social.entity.ActivityFeedItem;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi sự kiện thành tích lên activity_feed (FR-005, Q5=B):
 * chỉ streak_milestone (chuỗi tuần tăng + mốc 10/30/50/100) và new_pr (kỷ lục tổng khối lượng)
 * — KHÔNG phát từng buổi tập thường để tránh spam.
 *
 * Được gọi TRỰC TIẾP từ ProfileService.completeSession trong cùng transaction của buổi tập
 * (bọc try/catch phía caller): nếu buổi tập commit thì feed commit cùng; lỗi feed không làm
 * hỏng buổi tập. Không dùng AFTER_COMMIT event vì transaction mở trong callback afterCommit
 * không commit được (Spring quirk — đã kiểm chứng khi implement).
 */
@Service
public class ActivityFeedService {

    private static final int[] MILESTONE_WEEKS = {10, 30, 50, 100};
    private static final Pattern STREAK_PATTERN = Pattern.compile("\"streakWeeks\":(\\d+)");
    private static final Pattern VOLUME_PATTERN = Pattern.compile("\"volumeKg\":([0-9.]+)");

    private final ActivityFeedRepository feedRepository;

    public ActivityFeedService(ActivityFeedRepository feedRepository) {
        this.feedRepository = feedRepository;
    }

    /** FR-005: chỉ emit khi streak tăng; thêm 1 event khi chạm mốc đẹp (10/30/50/100). */
    @Transactional
    public void publishStreakMilestone(Long userId, int currentStreakWeeks) {
        if (currentStreakWeeks <= 0) {
            return;
        }
        ActivityFeedItem latest = feedRepository
                .findFirstByUserIdAndActionTypeOrderByIdDesc(userId, "streak_milestone")
                .orElse(null);
        int lastStreak = latest == null ? 0 : parseStreak(latest.getDetailsJson());
        if (currentStreakWeeks <= lastStreak) {
            return;
        }
        feedRepository.save(item(userId, "streak_milestone",
                "{\"streakWeeks\":" + currentStreakWeeks + "}"));
        for (int m : MILESTONE_WEEKS) {
            if (m > lastStreak && m <= currentStreakWeeks) {
                feedRepository.save(item(userId, "streak_milestone",
                        "{\"streakWeeks\":" + currentStreakWeeks + ",\"milestone\":" + m + "}"));
            }
        }
    }

    /** FR-005: phá kỷ lục tổng khối lượng (kg) của một buổi tập. */
    @Transactional
    public void publishPr(Long userId, BigDecimal totalVolumeKg) {
        if (totalVolumeKg == null || totalVolumeKg.signum() <= 0) {
            return;
        }
        ActivityFeedItem latest = feedRepository
                .findFirstByUserIdAndActionTypeOrderByIdDesc(userId, "new_pr")
                .orElse(null);
        BigDecimal last = latest == null ? BigDecimal.ZERO : parseVolume(latest.getDetailsJson());
        if (totalVolumeKg.compareTo(last) > 0) {
            feedRepository.save(item(userId, "new_pr",
                    "{\"volumeKg\":" + totalVolumeKg.stripTrailingZeros().toPlainString() + "}"));
        }
    }

    private ActivityFeedItem item(Long userId, String actionType, String detailsJson) {
        return ActivityFeedItem.builder()
                .userId(userId)
                .actionType(actionType)
                .detailsJson(detailsJson)
                .build();
    }

    private int parseStreak(String json) {
        Matcher m = STREAK_PATTERN.matcher(json == null ? "" : json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private BigDecimal parseVolume(String json) {
        Matcher m = VOLUME_PATTERN.matcher(json == null ? "" : json);
        return m.find() ? new BigDecimal(m.group(1)) : BigDecimal.ZERO;
    }
}
