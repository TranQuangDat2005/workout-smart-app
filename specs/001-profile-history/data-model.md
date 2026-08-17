# Data Model: Hồ sơ & Lịch sử tập (001-profile-history)

## Bảng tái sử dụng (không migration mới)

- `users` (V1) — cột `display_name`, `avatar_url`, `age`, `height_cm`, `goal_type`, `account_status`, `deleted_at` dùng cho hồ sơ & soft-delete.
- `workout_sessions` (V4) — lịch sử buổi tập: `id`, `user_id`, `start_time`, `end_time`, `status`, `focus_interruptions_count`.
- `workout_sets` (V4) — chi tiết hiệp: `id`, `session_id`, `set_number`, `reps_completed`, `weight_used`, `rest_time_seconds`.

## Entities mới

| Entity | Bảng | Ghi chú |
|---|---|---|
| `WorkoutSession` | workout_sessions | status: active/completed/interrupted/expired |
| `WorkoutSet` | workout_sets | |

## State transitions liên quan

```
active --DELETE /account--> deleted (set deleted_at; giữ nguyên mọi dữ liệu)
deleted --restore (007-core-auth)--> active
```

## Quy tắc nghiệp vụ

- Cân nặng KHÔNG sửa ở hồ sơ — đồng bộ từ body_metrics (002).
- Đổi goal_type → response `goalChanged=true` (việc tạo lại plan thuộc 008).
- Lịch sử phân trang 20/trang, sắp `start_time` DESC; totalVolumeKg = Σ(weight × reps).
