package com.workoutsmart.feed.repository;

import com.workoutsmart.feed.entity.PostComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findByPostIdOrderByIdAsc(Long postId);

    List<PostComment> findByPostIdIn(List<Long> postIds);
}
