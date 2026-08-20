-- V14: 015 time-based exercises — measure_type + duration
ALTER TABLE exercises ADD COLUMN measure_type VARCHAR(20) NOT NULL DEFAULT 'reps_weight';

ALTER TABLE workout_plan_exercises ADD COLUMN target_duration_seconds INT;
ALTER TABLE workout_session_exercises ADD COLUMN target_duration_seconds INT;
ALTER TABLE workout_session_exercises ADD COLUMN measure_type VARCHAR(20) NOT NULL DEFAULT 'reps_weight';

ALTER TABLE workout_plan_exercise_sets ADD COLUMN target_duration_seconds INT;
ALTER TABLE workout_session_exercise_sets ADD COLUMN target_duration_seconds INT;

ALTER TABLE workout_sets ADD COLUMN duration_seconds INT;
