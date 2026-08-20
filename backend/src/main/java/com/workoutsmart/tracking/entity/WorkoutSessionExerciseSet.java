package com.workoutsmart.tracking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Snapshot target một hiệp của buổi tập — FR-003 (014). */
@Entity
@Table(name = "workout_session_exercise_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSessionExerciseSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_exercise_id", nullable = false)
    private Long sessionExerciseId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "target_reps", nullable = false)
    private int targetReps;

    @Column(name = "target_weight")
    private BigDecimal targetWeight;

    @Column(name = "set_type", nullable = false)
    @Builder.Default
    private String setType = "normal";

    @Column(name = "target_duration_seconds")
    private Integer targetDurationSeconds;
}
