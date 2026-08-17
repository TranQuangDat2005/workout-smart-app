-- V9: Workout plan feature 008 — goal setup, rule engine inputs, draft queue
-- Schema extension on top of V1 (users), V3 (exercises), V5 (workout plans).

ALTER TABLE users ADD COLUMN equipment VARCHAR(500);

ALTER TABLE workout_plans ADD COLUMN fitness_level VARCHAR(30);
ALTER TABLE workout_plans ADD COLUMN effective_date DATE;

ALTER TABLE workout_plan_exercises ADD COLUMN rest_time_seconds INT NOT NULL DEFAULT 60;

CREATE TABLE draft_exercises (
    id                     BIGSERIAL PRIMARY KEY,
    session_id             BIGINT NOT NULL REFERENCES workout_sessions(id),
    original_exercise_id   BIGINT NOT NULL REFERENCES exercises(id),
    cloned_exercise_id     BIGINT NOT NULL REFERENCES exercises(id),
    replacement_exercise_id BIGINT REFERENCES exercises(id),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_draft_exercises_session ON draft_exercises (session_id);
