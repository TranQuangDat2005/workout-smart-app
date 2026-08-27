package com.workoutsmart.social.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "friendships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_1", nullable = false)
    private Long userId1;

    @Column(name = "user_id_2", nullable = false)
    private Long userId2;

    /** pending / accepted / rejected. */
    @Column(nullable = false)
    private String status;

    @Column(name = "initiated_by", nullable = false)
    private Long initiatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Cặp chuẩn hóa (min, max) — nền của unique index V20. */
    @Column(name = "pair_min")
    private Long pairMin;

    @Column(name = "pair_max")
    private Long pairMax;

    /** 1 = bản ghi hoạt động; NULL = superseded (dedupe V20). */
    @Column(name = "active_marker")
    private Integer activeMarker;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (userId1 != null && userId2 != null) {
            pairMin = Math.min(userId1, userId2);
            pairMax = Math.max(userId1, userId2);
        }
        if (activeMarker == null) {
            activeMarker = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
