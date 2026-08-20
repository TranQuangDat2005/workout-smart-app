# Data Model: Hiệp tập nâng cao (014)

**Migration mới**: `V13__advanced_sets.sql` (additive, không sửa migration cũ).

## 1. Bảng mới `workout_plan_exercise_sets` (target từng hiệp — template)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `plan_exercise_id` | BIGINT | FK → `workout_plan_exercises(id)` ON DELETE CASCADE, NOT NULL | |
| `set_number` | INT | NOT NULL | 1-based |
| `target_reps` | INT | NOT NULL | |
| `target_weight` | NUMERIC(8,2) | NULL | optional |
| `set_type` | VARCHAR(20) | NOT NULL DEFAULT 'normal' | normal/warm_up/drop_set |

- `UNIQUE(plan_exercise_id, set_number)`.

## 2. Bảng mới `workout_session_exercise_sets` (target từng hiệp — snapshot)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `session_exercise_id` | BIGINT | FK → `workout_session_exercises(id)` ON DELETE CASCADE, NOT NULL | |
| `set_number` | INT | NOT NULL | |
| `target_reps` | INT | NOT NULL | |
| `target_weight` | NUMERIC(8,2) | NULL | |
| `set_type` | VARCHAR(20) | NOT NULL DEFAULT 'normal' | |

- `UNIQUE(session_exercise_id, set_number)`.

## 3. Thay đổi `workout_sets` (thực tế)

Thêm cột `set_type VARCHAR(20) NOT NULL DEFAULT 'normal'`.

- `reps_completed`/`weight_used` vẫn là dữ liệu thực tế (nullable cho đến khi ghi).
- UPSERT giữ nguyên `(session_id, session_exercise_id, set_number)` — không đổi invariant sync (constitution §4).

## 4. Quy tắc

- Khi `replaceDayExercises` nhận `sets[]` không rỗng → xóa target cũ (cascade theo `plan_exercise`) và lưu target mới theo thứ tự.
- Khi `sets[]` rỗng/thiếu → không tạo bảng con; dùng `target_sets`/`target_reps` chung.
- Snapshot copy target từng hiệp từ `workout_plan_exercise_sets` → `workout_session_exercise_sets` khi có; ngược lại để rỗng và UI fallback `target_sets`/`target_reps`.
- `set_type` hợp lệ: `normal` | `warm_up` | `drop_set`; sai → 400.

## 5. Quan hệ

```text
workout_plan_exercises 1 ──── n workout_plan_exercise_sets
workout_session_exercises 1 ──── n workout_session_exercise_sets
workout_sets (set_type) — hiệp thực tế gắn session_exercise_id
```
