# Quickstart Validation: Thiết lập mục tiêu & Tạo lộ trình tập (008-workout-plan)

Hướng dẫn kiểm chứng feature chạy đúng end-to-end. Chi tiết kỹ thuật xem [data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml). Code thực thi nằm trong `tasks.md` (bước tiếp theo `/speckit-tasks`).

## Prerequisites

- PostgreSQL 18 chạy local (hoặc docker), đã chạy Flyway migration V1 + V3 (bảng `exercises`, `workout_plans`, `workout_plan_days`, `workout_plan_exercises`, `draft_exercises`).
- Exercise library đã import: 1324+ rows trong bảng `exercises` (Admin import trước khi test).
- Backend Spring Boot: `cd backend && ./mvnw spring-boot:run`
- Web: `cd web && npm run dev` · Mobile: `flutter run` (tùy theo client đang kiểm).
- User đã đăng nhập (có JWT access token hợp lệ).

## Các kịch bản kiểm chứng

### 1. Thiết lập mục tiêu + Tạo plan tự động (User Story 1 & 2)

1. User mới (chưa có goal) gọi `PUT /api/v1/users/me/goals` với `{goalType: "weight_loss", fitnessLevel: "beginner", equipment: ["body_weight"]}` → expect **200** + response chứa `planId`.
2. Verify trong DB: `workout_plans` có row `status='active'`, `goal_type='weight_loss'`, `fitness_level='beginner'`.
3. Verify trong DB: `workout_plan_days` có 4-5 rows (weight_loss = 4-5 ngày/tuần), mỗi row có `day_of_week` khác nhau.
4. Verify trong DB: `workout_plan_exercises` mỗi day có 2-3 rows (beginner = 2-3 bài/ngày), mỗi row có `target_sets=3`, `target_reps` 12-15, `rest_time_seconds` 45-60.
5. Verify: tất cả exercises trong plan đều có `equipment='body_weight'` (khớp equipment user chọn).

### 2. Xem lộ trình tập (User Story 2)

1. Gọi `GET /api/v1/workout-plans/active` → expect **200** + plan response chứa danh sách days với exercises.
2. Nếu chưa có plan active → expect **404**.

### 3. Thay đổi mục tiêu — tạo plan mới (FR-005)

1. User đã có plan active với `goal_type='weight_loss'`. Gọi `PUT /api/v1/users/me/goals` với `{goalType: "muscle_gain", fitnessLevel: "intermediate", equipment: ["dumbbell", "barbell"]}`.
2. Nếu gửi `effectiveDate` = hôm nay → plan cũ `status='archived'`, plan mới `status='active'` ngay lập tức.
3. Nếu gửi `effectiveDate` = ngày mai → plan cũ giữ `active` đến hết ngày hôm nay, plan mới `active` từ ngày mai.
4. Verify: plan mới có exercises với `equipment` chứa dumbbell/barbell, `target_sets=4`, `target_reps` 8-12.

### 4. Tìm kiếm bài tập (User Story 3 — FR-003, FR-006)

1. Gọi `GET /api/v1/exercises?equipment=body_weight` → expect **200** + chỉ trả bài `equipment='body_weight'` và `status='active'`.
2. Gọi `GET /api/v1/exercises?muscleGroup=chest` → expect **200** + trả bài thuộc nhóm cơ ngực.
3. Gọi `GET /api/v1/exercises?page=0&size=10` → expect **200** + response chứa `totalElements`, `totalPages`.

### 5. Xem chi tiết bài tập (User Story 3 — FR-004, FR-007)

1. Gọi `GET /api/v1/exercises/{exerciseId}` với ID hợp lệ → expect **200** + response chứa `name`, `gifUrl`, `image`, `instructions`.
2. Nếu `gifUrl` là null → client tự fallback sang `image` (kiểm tra ở client-side).
3. Nếu `exerciseId` không tồn tại → expect **404**.

### 6. Bài tập bị ẩn trong active plan (FR-011)

1. User có active plan chứa exercise X. Admin set `exercises.status='inactive'` cho exercise X.
2. User mở workout session (hoặc system detect khi plan query) → expect: system tạo row trong `draft_exercises` với `original_exercise_id=X`, `cloned_exercise_id` là bản copy.
3. System tìm `replacement_exercise_id` (cùng muscle_group + body_part) nếu có.
4. User hoàn thành session → `draft_exercises` rows có `session_id` đó bị xóa.

## Lệnh test tự động

```bash
# Backend (≥80% coverage phần workout plan)
cd backend && ./mvnw test

# Web
cd web && npm test

# Mobile
cd mobile && flutter test
```

## Expected outcomes (tóm tắt)

| Tiêu chí | Kỳ vọng |
|---|---|
| SC-001 | Plan generation hoàn tất < 5 giây sau goal setup |
| SC-002 | 100% exercises trong plan khớp đúng goal_type + equipment |
| SC-003 | Exercise detail API response < 300ms (P95) |
| SC-004 | User có thể xem plan với < 3 taps từ screen chính (client-side) |
| SC-005 | 100% plan tuân thủ Rule Engine v1 (số ngày, reps, rest time, sets) |
