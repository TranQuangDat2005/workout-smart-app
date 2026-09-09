package com.workoutsmart.social.repository;

import com.workoutsmart.social.entity.ActivityFeedItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityFeedRepository extends JpaRepository<ActivityFeedItem, Long> {

    /** Feed bạn bè — 7 ngày gần nhất (FR-006), mới nhất trước, tối đa 50. */
    List<ActivityFeedItem> findTop50ByUserIdInAndCreatedAtAfterOrderByCreatedAtDesc(
            List<Long> userIds, Instant since);

    /** Event gần nhất của một loại — dùng để dedupe milestone/PR (FR-005). */
    Optional<ActivityFeedItem> findFirstByUserIdAndActionTypeOrderByIdDesc(
            Long userId, String actionType);
}
