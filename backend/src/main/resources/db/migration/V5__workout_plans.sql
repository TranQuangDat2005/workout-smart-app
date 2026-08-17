-- V5: Workout plans — theo General Spec §5 (UC-04, UC-05)
CREATE TABLE workout_plans (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    name       VARCHAR(255),
    goal_type  VARCHAR(30),
    status     VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_plans_user ON workout_plans (user_id, status);

CREATE TABLE workout_plan_days (
    id          BIGSERIAL PRIMARY KEY,
    plan_id     BIGINT NOT NULL REFERENCES workout_plans(id),
    day_of_week INT NOT NULL
);

CREATE INDEX idx_plan_days_plan ON workout_plan_days (plan_id);

CREATE TABLE workout_plan_exercises (
    id           BIGSERIAL PRIMARY KEY,
    day_id       BIGINT NOT NULL REFERENCES workout_plan_days(id),
    exercise_id  BIGINT NOT NULL REFERENCES exercises(id),
    target_sets  INT NOT NULL DEFAULT 3,
    target_reps  INT NOT NULL DEFAULT 12
);

CREATE INDEX idx_plan_exercises_day ON workout_plan_exercises (day_id);
