-- V3: Bảng exercises — theo specs/General Spec.md §5 (UC-06, UC-19)
CREATE TABLE exercises (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    category      VARCHAR(100),
    body_part     VARCHAR(100),
    equipment     VARCHAR(100),
    target        VARCHAR(100),
    muscle_group  VARCHAR(100),
    image         VARCHAR(500),
    gif_url       VARCHAR(500),
    instructions  TEXT,
    status        VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_exercises_status ON exercises (status);
CREATE INDEX idx_exercises_equipment ON exercises (equipment);
