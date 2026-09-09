-- V22: 018-fix-social-flows — hồ sơ riêng tư (FR-013).
-- Field do 001-profile-history sở hữu; mặc định private đúng General Spec §3.
ALTER TABLE users ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT TRUE;
