package com.workoutsmart.tracking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workout_session_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSessionExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "target_sets", nullable = false)
    private int targetSets;

    @Column(name = "target_reps", nullable = false)
    private int targetReps;

    @Column(name = "rest_time_seconds", nullable = false)
    private int restTimeSeconds;

    @Column(name = "exercise_name")
    private String exerciseName;

    @Column(name = "target_duration_seconds")
    private Integer targetDurationSeconds;

    @Column(name = "measure_type", nullable = false)
    @Builder.Default
    private String measureType = "reps_weight";
}
