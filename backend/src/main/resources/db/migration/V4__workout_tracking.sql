-- V4: Workout tracking — theo General Spec §5 (UC-07..09)
CREATE TABLE workout_sessions (
    id                        BIGSERIAL PRIMARY KEY,
    user_id                   BIGINT NOT NULL REFERENCES users(id),
    plan_id                   BIGINT,
    start_time                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    end_time                  TIMESTAMP WITH TIME ZONE,
    status                    VARCHAR(20) NOT NULL DEFAULT 'active',
    focus_interruptions_count INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sessions_user ON workout_sessions (user_id, start_time);

CREATE TABLE workout_sets (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT NOT NULL REFERENCES workout_sessions(id),
    exercise_id     BIGINT,
    set_number      INT NOT NULL,
    reps_completed  INT,
    weight_used     NUMERIC(8,2),
    rest_time_seconds INT,
    UNIQUE (session_id, set_number)
);

CREATE INDEX idx_sets_session ON workout_sets (session_id);
