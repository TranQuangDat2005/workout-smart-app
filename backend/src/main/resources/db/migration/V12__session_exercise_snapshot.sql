-- V12: 013 manual workout builder — sort_order template + session snapshot + unique sets per bài
ALTER TABLE workout_plan_exercises ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

CREATE TABLE workout_session_exercises (
    id                 BIGSERIAL PRIMARY KEY,
    session_id         BIGINT NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id        BIGINT NOT NULL REFERENCES exercises(id),
    sort_order         INT NOT NULL DEFAULT 0,
    target_sets        INT NOT NULL,
    target_reps        INT NOT NULL,
    rest_time_seconds  INT NOT NULL DEFAULT 60,
    exercise_name      VARCHAR(255)
);

CREATE INDEX idx_session_exercises_session ON workout_session_exercises (session_id);

ALTER TABLE workout_sets ADD COLUMN session_exercise_id BIGINT REFERENCES workout_session_exercises(id) ON DELETE SET NULL;

ALTER TABLE workout_sets DROP CONSTRAINT IF EXISTS workout_sets_session_id_set_number_key;

CREATE UNIQUE INDEX uq_workout_sets_session_ex_set
    ON workout_sets (session_id, session_exercise_id, set_number);
