-- V2: Bảng auth — theo specs/007-core-auth/data-model.md §2-3

CREATE TABLE otp_verifications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    purpose         VARCHAR(30)  NOT NULL,
    code_hash       VARCHAR(100) NOT NULL,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts_count  INT          NOT NULL DEFAULT 0,
    used_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_otp_user_purpose ON otp_verifications (user_id, purpose, created_at);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_token_user ON refresh_tokens (user_id);
