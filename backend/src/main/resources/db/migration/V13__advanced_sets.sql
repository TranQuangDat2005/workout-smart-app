-- V13: 014 advanced sets — per-set target + set_type
CREATE TABLE workout_plan_exercise_sets (
    id               BIGSERIAL PRIMARY KEY,
    plan_exercise_id BIGINT NOT NULL REFERENCES workout_plan_exercises(id) ON DELETE CASCADE,
    set_number       INT NOT NULL,
    target_reps      INT NOT NULL,
    target_weight    NUMERIC(8,2),
    set_type         VARCHAR(20) NOT NULL DEFAULT 'normal',
    CONSTRAINT uq_plan_exercise_set UNIQUE (plan_exercise_id, set_number)
);

CREATE INDEX idx_plan_exercise_sets ON workout_plan_exercise_sets (plan_exercise_id);

CREATE TABLE workout_session_exercise_sets (
    id                  BIGSERIAL PRIMARY KEY,
    session_exercise_id BIGINT NOT NULL REFERENCES workout_session_exercises(id) ON DELETE CASCADE,
    set_number          INT NOT NULL,
    target_reps         INT NOT NULL,
    target_weight       NUMERIC(8,2),
    set_type            VARCHAR(20) NOT NULL DEFAULT 'normal',
    CONSTRAINT uq_session_exercise_set UNIQUE (session_exercise_id, set_number)
);

CREATE INDEX idx_session_exercise_sets ON workout_session_exercise_sets (session_exercise_id);

ALTER TABLE workout_sets ADD COLUMN set_type VARCHAR(20) NOT NULL DEFAULT 'normal';
