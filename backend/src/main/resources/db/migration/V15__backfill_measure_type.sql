-- V15: backfill measure_type + target duration cho bài tập theo thời gian đã tồn tại
UPDATE exercises
SET measure_type = 'duration'
WHERE LOWER(category) IN ('cardio', 'stretching')
   OR LOWER(name) LIKE '%plank%'
   OR LOWER(name) LIKE '%wall sit%'
   OR LOWER(name) LIKE '%hold%'
   OR LOWER(name) LIKE '%bridge%'
   OR LOWER(name) LIKE '%superman%';

UPDATE workout_plan_exercises
SET target_reps = 0,
    target_duration_seconds = 1200
WHERE exercise_id IN (
    SELECT id FROM exercises
    WHERE measure_type = 'duration' AND LOWER(category) = 'cardio'
);

UPDATE workout_plan_exercises
SET target_reps = 0,
    target_duration_seconds = 60
WHERE exercise_id IN (
    SELECT id FROM exercises
    WHERE measure_type = 'duration' AND LOWER(category) <> 'cardio'
);
