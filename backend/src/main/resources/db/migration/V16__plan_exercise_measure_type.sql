-- V16: 015b — cho phép mỗi bài trong plan ghi đè đơn vị theo dõi (reps_weight / duration)
-- NULL = dùng measure_type mặc định của bài thư viện.
ALTER TABLE workout_plan_exercises ADD COLUMN measure_type VARCHAR(20);
