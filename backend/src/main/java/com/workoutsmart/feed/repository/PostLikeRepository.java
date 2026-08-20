package com.workoutsmart.feed.repository;

import com.workoutsmart.feed.entity.PostLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    List<PostLike> findByPostIdIn(List<Long> postIds);

    long countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, Long userId);
}
