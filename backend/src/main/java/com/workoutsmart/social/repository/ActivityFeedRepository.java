package com.workoutsmart.social.repository;

import com.workoutsmart.social.entity.ActivityFeedItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityFeedRepository extends JpaRepository<ActivityFeedItem, Long> {

    List<ActivityFeedItem> findTop50ByUserIdInOrderByCreatedAtDesc(List<Long> userIds);
}
