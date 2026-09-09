-- V21: 018-fix-social-flows — tie-break leaderboard theo "ai đạt chuỗi hiện tại sớm hơn" (FR-007).
ALTER TABLE leaderboard_entries ADD COLUMN streak_start_week DATE;
