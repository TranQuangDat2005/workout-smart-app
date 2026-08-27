package com.workoutsmart.feed.repository;

import com.workoutsmart.feed.entity.CommunityPost;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    /** Feed "Bạn bè": bài public toàn app + bài (public/friends) của bạn bè + bài của chính mình. */
    @Query("""
            select p from CommunityPost p
            where (:cursor is null or p.id < :cursor)
              and (p.audience = 'public'
                   or p.userId = :me
                   or (p.userId in :friendIds and p.audience in ('public', 'friends')))
            order by p.id desc
            """)
    List<CommunityPost> findFeed(@Param("me") Long me,
                                 @Param("friendIds") List<Long> friendIds,
                                 @Param("cursor") Long cursor,
                                 Pageable pageable);

    /** Feed "Khám phá": toàn bộ bài public, trừ bài của user private (FR-015). */
    @Query("""
            select p from CommunityPost p
            where (:cursor is null or p.id < :cursor) and p.audience = 'public'
              and not exists (select 1 from com.workoutsmart.auth.entity.User u
                              where u.id = p.userId and u.isPrivate = true)
            order by p.id desc
            """)
    List<CommunityPost> findDiscover(@Param("cursor") Long cursor, Pageable pageable);
}
