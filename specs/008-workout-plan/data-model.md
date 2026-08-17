# Data Model: Thiết lập mục tiêu & Tạo lộ trình tập (008-workout-plan)

**Feature**: specs/008-workout-plan | **Date**: 2026-08-17

## Tổng quan

3 bảng mới liên quan feature này: `workout_plans`, `workout_plan_days`, `workout_plan_exercises`. Bảng `exercises` đã có từ General Spec (không thêm cột mới trong feature này). Bảng `draft_exercises` (mới) cho draft queue. Migration Flyway: `V3__workout_plan_tables.sql` (bảng `exercises` nằm ở `V1__init.sql`).

## 1. Bảng `exercises` (đã có — không thay đổi)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `name` | VARCHAR(255) | NOT NULL | Tên bài tập |
| `category` | VARCHAR(50) | NOT NULL | cardio, waist, strength, stretching... |
| `body_part` | VARCHAR(50) | NOT NULL | Bộ phận cơ thể chính |
| `equipment` | VARCHAR(50) | NOT NULL | dumbbell, barbell, body_weight, machine... |
| `target` | VARCHAR(100) | NULL | Mục tiêu cụ thể (target muscle) |
| `muscle_group` | VARCHAR(50) | NOT NULL | Nhóm cơ: chest, back, legs, shoulders, arms, core |
| `image` | VARCHAR(500) | NULL | URL ảnh tĩnh 180×180 |
| `gif_url` | VARCHAR(500) | NULL | URL ảnh động GIF |
| `instructions` | JSONB | NULL | Hướng dẫn đa ngôn ngữ, ví dụ: `{"en": ["Step 1...", "Step 2..."]}` |
| `status` | VARCHAR(20) | NOT NULL, default 'active' | active / inactive |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `updated_at` | TIMESTAMPTZ | NOT NULL | |

Quy tắc nghiệp vụ:
- Exercise search chỉ trả bài có `status = 'active'` (FR-006).
- Exercise bị ẩn (inactive) vẫn hiển thị trong lịch sử tập cũ.
- Bulk import by Admin: duplicate detection theo `(name, equipment)` normalized; re-import = upsert.

## 2. Bảng `workout_plans` (mới)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK → users.id, NOT NULL, INDEX | Mỗi user tối đa 1 plan active |
| `name` | VARCHAR(255) | NOT NULL | Tên plan, ví dụ: "Giảm cân - Tuần 1" |
| `goal_type` | VARCHAR(30) | NOT NULL | weight_loss / muscle_gain / endurance |
| `fitness_level` | VARCHAR(30) | NOT NULL | beginner / intermediate / advanced |
| `status` | VARCHAR(20) | NOT NULL, default 'active' | active / archived |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `updated_at` | TIMESTAMPTZ | NOT NULL | |

Quy tắc nghiệp vụ:
- Mỗi user chỉ có 1 plan `status = 'active'` tại thời điểm (UNIQUE partial index trên `(user_id) WHERE status = 'active'`).
- Khi tạo plan mới: plan cũ tự chuyển `archived` (FR-005).
- `goal_type` + `fitness_level` + `equipment` từ user profile được dùng làm input Rule Engine.

### State transitions (`status`)

```
created → active (khi Rule Engine sinh xong plan)
active ── User thay goal_type ──→ archived
active ── User tạo plan mới ──→ archived
archived ── KHÔNG thể quay lại active
```

## 3. Bảng `workout_plan_days` (mới)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `plan_id` | BIGINT | FK → workout_plans.id, NOT NULL, INDEX | |
| `day_of_week` | INT | NOT NULL | 0=Chủ nhật, 1=Thứ 2, ..., 6=Thứ 7 |
| `created_at` | TIMESTAMPTZ | NOT NULL | |

Quy tắc nghiệp vụ:
- Mỗi plan có N `workout_plan_days` tương ứng số ngày/tuần theo goal_type (FR-008).
- `day_of_week` UNIQUE trong cùng 1 plan (mỗi plan chỉ có 1 row cho mỗi ngày trong tuần).

## 4. Bảng `workout_plan_exercises` (mới)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `day_id` | BIGINT | FK → workout_plan_days.id, NOT NULL, INDEX | |
| `exercise_id` | BIGINT | FK → exercises.id, NOT NULL | Chỉ bài active khi tạo plan |
| `target_sets` | INT | NOT NULL | Số sets mục tiêu (theo fitness_level) |
| `target_reps` | INT | NOT NULL | Số reps mục tiêu (theo goal_type) |
| `rest_time_seconds` | INT | NOT NULL | Thời gian nghỉ (theo goal_type) |
| `created_at` | TIMESTAMPTZ | NOT NULL | |

Quy tắc nghiệp vụ:
- `target_sets` × `target_reps` theo bảng luật Rule Engine v1.
- `rest_time_seconds` theo goal_type: weight_loss=45-60, muscle_gain=60-90, endurance=30-45 (FR-008).
- Khi bài tập bị Admin ẩn: hệ thống thông báo + clone vào draft queue (FR-011).

## 5. Bảng `draft_exercises` (mới)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `session_id` | BIGINT | FK → workout_sessions.id, NOT NULL, INDEX | Buổi tập đang diễn ra |
| `original_exercise_id` | BIGINT | FK → exercises.id, NOT NULL | Bài gốc bị ẩn |
| `cloned_exercise_id` | BIGINT | FK → exercises.id, NOT NULL | Bản clone để user tập tiếp |
| `replacement_exercise_id` | BIGINT | FK → exercises.id, NULL | Gợi ý thay thế (nếu tìm được) |
| `created_at` | TIMESTAMPTZ | NOT NULL | |

Quy tắc nghiệp vụ:
- Khi Admin ẩn exercise trong active plan → system tạo row trong `draft_exercises` cho session hiện tại.
- Khi workout session hoàn thành → xóa tất cả row `draft_exercises` có `session_id` đó.
- `cloned_exercise_id` là bản copy của `original_exercise_id` tại thời điểm ẩn (để user tập nốt buổi).
- `replacement_exercise_id`: query exercise cùng muscle_group + body_part, limit 1.

## 6. Quan hệ

```text
users 1 ──── n workout_plans
workout_plans 1 ──── n workout_plan_days
workout_plan_days 1 ──── n workout_plan_exercises
exercises 1 ──── n workout_plan_exercises (exercise_id)
workout_sessions 1 ──── n draft_exercises (session_id)
exercises 1 ──── n draft_exercises (original_exercise_id)
exercises 1 ──── n draft_exercises (cloned_exercise_id)
exercises 1 ──── n draft_exercises (replacement_exercise_id, nullable)
```

## 7. Migration impact

- `V1__init.sql` (đã có): bảng `exercises` — KHÔNG thay đổi.
- `V3__workout_plan_tables.sql` (mới): tạo `workout_plans`, `workout_plan_days`, `workout_plan_exercises`, `draft_exercises` + indexes:
  - `idx_workout_plans_user_id` ON `workout_plans(user_id)`
  - `uniq_active_plan_per_user` UNIQUE ON `workout_plans(user_id) WHERE status = 'active'`
  - `idx_plan_days_plan_id` ON `workout_plan_days(plan_id)`
  - `uniq_day_per_plan` UNIQUE ON `workout_plan_days(plan_id, day_of_week)`
  - `idx_plan_exercises_day_id` ON `workout_plan_exercises(day_id)`
  - `idx_plan_exercises_exercise_id` ON `workout_plan_exercises(exercise_id)`
  - `idx_draft_exercises_session_id` ON `draft_exercises(session_id)`
- Không sửa migration đã chạy (constitution §7).
