-- V11: Bài tập tự tạo cá nhân (custom exercise) — feature 011
ALTER TABLE exercises ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'system';
ALTER TABLE exercises ADD COLUMN created_by BIGINT NULL;
ALTER TABLE exercises ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE NULL;

CREATE INDEX idx_exercises_source ON exercises (source);
CREATE INDEX idx_exercises_created_by ON exercises (created_by);
CREATE INDEX idx_exercises_deleted_at ON exercises (deleted_at);
