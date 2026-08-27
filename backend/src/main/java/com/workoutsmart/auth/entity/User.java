package com.workoutsmart.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "goal_type")
    private String goalType;

    @Column(name = "fitness_level")
    private String fitnessLevel;

    /** Danh sách dụng cụ sẵn có, phân tách bằng dấu phẩy: body_weight,dumbbell,... */
    private String equipment;

    private String sex;

    @Column(name = "activity_level")
    private String activityLevel;

    /**
     * Mức điều chỉnh calo (019): maintain | cut_light (-300) | cut_fast (-500) |
     * bulk_light (+300) | bulk_fast (+500) | custom (dùng custom_calorie_offset).
     * Độc lập với goal_type (mục tiêu tập).
     */
    @Column(name = "calorie_goal", nullable = false)
    @Builder.Default
    private String calorieGoal = "maintain";

    /** Offset calo tùy chỉnh (kcal/ngày) — chỉ có hiệu lực khi calorie_goal = "custom". */
    @Column(name = "custom_calorie_offset")
    private Integer customCalorieOffset;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private Integer age;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** Hồ sơ riêng tư (V22) — spec 001 sở hữu; mặc định private (General Spec §3). */
    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private boolean isPrivate = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
