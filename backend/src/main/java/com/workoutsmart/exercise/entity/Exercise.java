package com.workoutsmart.exercise.entity;

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
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(name = "body_part")
    private String bodyPart;

    private String equipment;

    private String target;

    @Column(name = "muscle_group")
    private String muscleGroup;

    private String image;

    @Column(name = "gif_url")
    private String gifUrl;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    /** active = hiển thị; inactive = bị Admin ẩn (loại khỏi Rule Engine + tìm kiếm). */
    @Column(nullable = false)
    private String status;

    /** system = kho hệ thống; user_custom = bài tập cá nhân do User tạo. */
    @Builder.Default
    @Column(nullable = false)
    private String source = "system";

    /** Id của chủ sở hữu (chỉ có với source = user_custom). */
    @Column(name = "created_by")
    private Long createdBy;

    /** Soft-delete cho bài tập cá nhân; giữ 1 tuần rồi xóa cứng. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** reps_weight = đo bằng reps/tạ (mặc định); duration = đo bằng thời gian (giây). */
    @Column(name = "measure_type", nullable = false)
    @Builder.Default
    private String measureType = "reps_weight";

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
