package com.workoutsmart.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workout_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "exercise_id")
    private Long exerciseId;

    @Column(name = "set_number", nullable = false)
    private int setNumber;

    @Column(name = "reps_completed")
    private Integer repsCompleted;

    @Column(name = "weight_used")
    private BigDecimal weightUsed;

    @Column(name = "rest_time_seconds")
    private Integer restTimeSeconds;

    @Column(name = "client_timestamp")
    private Instant clientTimestamp;

    @Column(name = "session_exercise_id")
    private Long sessionExerciseId;

    /** normal / warm_up / drop_set. */
    @Column(name = "set_type", nullable = false)
    @Builder.Default
    private String setType = "normal";

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
