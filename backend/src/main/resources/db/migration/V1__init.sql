-- V1: Bảng users — theo specs/007-core-auth/data-model.md §1
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'user',
    goal_type       VARCHAR(30),
    fitness_level   VARCHAR(30),
    sex             VARCHAR(10),
    activity_level  VARCHAR(20),
    display_name    VARCHAR(100),
    avatar_url      VARCHAR(500),
    age             INT,
    weight_kg       NUMERIC(5,2),
    height_cm       NUMERIC(5,2),
    account_status  VARCHAR(20)  NOT NULL DEFAULT 'active',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
