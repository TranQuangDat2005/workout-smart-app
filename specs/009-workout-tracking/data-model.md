# Data Model: Theo dõi buổi tập (009-workout-tracking)

Không có migration mới. Reuse:
- `workout_sessions` (V4): `id`, `user_id`, `plan_id`, `start_time`, `end_time`, `status`, `focus_interruptions_count`.
- `workout_sets` (V4): `id`, `session_id`, `exercise_id`, `set_number`, `reps_completed`, `weight_used`, `rest_time_seconds` + `UNIQUE(session_id, set_number)`.

## State transition (`workout_sessions.status`)
```
(created) → active → completed
active ── Admin ban ──→ interrupted
active ── sang ngày mới ──→ expired
```

## Quy tắc
- Ghi hiệp = UPSERT theo `(session_id, set_number)` (Last-Write-Wins).
- `focus_interruptions_count` tăng dần từ client signal.
