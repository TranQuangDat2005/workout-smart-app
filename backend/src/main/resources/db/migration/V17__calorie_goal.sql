-- V17: 019 nutrition needs — mức điều chỉnh calo theo nhu cầu cá nhân
-- maintain = giữ nguyên TDEE; cut_light = -300; cut_fast = -500; bulk_light = +300; bulk_fast = +500
ALTER TABLE users ADD COLUMN calorie_goal VARCHAR(20) NOT NULL DEFAULT 'maintain';
