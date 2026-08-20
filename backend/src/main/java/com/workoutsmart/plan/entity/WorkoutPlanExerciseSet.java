package com.workoutsmart.plan.entity;

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

/** Target một hiệp trong template — FR-001 (014). */
@Entity
@Table(name = "workout_plan_exercise_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutPlanExerciseSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_exercise_id", nullable = false)
    private Long planExerciseId;

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
