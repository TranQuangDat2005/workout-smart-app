# Data Model: Quản trị hệ thống (006-admin-management)

Không có migration mới. Reuse các bảng đã có:
- `users` (V1): `account_status` (active/banned/deleted).
- `exercises` (V3): `status` (active/inactive) + các trường nội dung.
- `workout_sessions` (V4): lịch sử tập để Admin xem.
- `audit_logs` (V8): `admin_id`, `action_type`, `target_type`, `target_id`, `reason`, `details_json`, `created_at`.

## Quan hệ
```text
users 1 ──── n workout_sessions
users 1 ──── n audit_logs (admin_id)
exercises 1 ──── n audit_logs (target_id = exercise)
```

## State transition
- `users.account_status`: `active → banned → active` (Admin ban/unban).
- `exercises.status`: `active ↔ inactive` (Admin ẩn/hiện).
