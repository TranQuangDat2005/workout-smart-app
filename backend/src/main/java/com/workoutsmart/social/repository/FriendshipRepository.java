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

    /**
     * Bản ghi HOẠT ĐỘNG giữa 2 user (không phân biệt chiều).
     * Trả List thay vì Optional để không ném ngoại lệ khi dữ liệu cũ còn nhiều row
     * (sau V20 luôn ≤1 bản ghi hoạt động/pair — xem FR-001).
     */
    @Query("SELECT f FROM Friendship f WHERE f.activeMarker = 1 AND "
            + "((f.userId1 = :a AND f.userId2 = :b) OR (f.userId1 = :b AND f.userId2 = :a))")
    List<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);

    /** Batch trạng thái quan hệ giữa :me và danh sách :ids (FR-004 — tránh N+1). */
    @Query("SELECT f FROM Friendship f WHERE f.activeMarker = 1 AND "
            + "((f.userId1 = :me AND f.userId2 IN :ids) OR (f.userId2 = :me AND f.userId1 IN :ids))")
    List<Friendship> findWithAnyOf(@Param("me") Long me, @Param("ids") List<Long> ids);

    @Query("SELECT f FROM Friendship f WHERE f.userId2 = :userId AND f.status = 'pending' "
            + "AND f.activeMarker = 1")
    List<Friendship> findPendingFor(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.activeMarker = 1 AND "
            + "((f.userId1 = :userId) OR (f.userId2 = :userId)) AND f.status = 'accepted'")
    List<Friendship> findAcceptedFor(@Param("userId") Long userId);

    /** Số bạn bè hiện tại — chặn khi đạt 500 (FR-003). */
    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.activeMarker = 1 AND "
            + "((f.userId1 = :userId) OR (f.userId2 = :userId)) AND f.status = 'accepted'")
    long countAcceptedFor(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.initiatedBy = :userId "
            + "AND f.userId2 = :target AND f.status = 'rejected' AND f.activeMarker = 1 "
            + "AND f.updatedAt > :since")
    long countRejectedSince(@Param("userId") Long userId, @Param("target") Long target,
                            @Param("since") Instant since);
}
