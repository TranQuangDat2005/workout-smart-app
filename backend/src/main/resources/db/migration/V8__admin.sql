-- V8: Admin — theo General Spec §5 (UC-18)
CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    admin_id     BIGINT,
    action_type  VARCHAR(50) NOT NULL,
    target_type  VARCHAR(20),
    target_id    BIGINT,
    reason       VARCHAR(500),
    details_json TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_admin ON audit_logs (admin_id, created_at);
