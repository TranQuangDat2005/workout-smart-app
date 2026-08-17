package com.workoutsmart.social.repository;

import com.workoutsmart.social.entity.Friendship;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByUserId1AndUserId2(Long userId1, Long userId2);

    /** Bất kỳ friendship nào giữa 2 user (không phân biệt chiều). */
    @Query("SELECT f FROM Friendship f WHERE "
            + "(f.userId1 = :a AND f.userId2 = :b) OR (f.userId1 = :b AND f.userId2 = :a)")
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("SELECT f FROM Friendship f WHERE f.userId2 = :userId AND f.status = 'pending'")
    List<Friendship> findPendingFor(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE "
            + "((f.userId1 = :userId) OR (f.userId2 = :userId)) AND f.status = 'accepted'")
    List<Friendship> findAcceptedFor(@Param("userId") Long userId);

    long countByInitiatedByAndCreatedAtAfter(Long initiatedBy, Instant since);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.initiatedBy = :userId "
            + "AND f.userId2 = :target AND f.status = 'rejected' AND f.updatedAt > :since")
    long countRejectedSince(@Param("userId") Long userId, @Param("target") Long target,
                            @Param("since") Instant since);
}
