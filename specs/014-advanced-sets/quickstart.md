# Quickstart: Hiệp tập nâng cao (014)

## Prerequisites

- User đã đăng nhập, có lộ trình active.
- Backend chạy (Flyway đã áp dụng `V13__advanced_sets.sql`).

## Validate đặt target từng hiệp (US1)

1. `PUT /api/v1/workout-plans/active/days/{dayId}/exercises` với một bài có `sets`:
   `[{setNumber:1,targetReps:12},{setNumber:2,targetReps:10},{setNumber:3,targetReps:8},{setNumber:4,targetReps:6}]`.
2. `GET /api/v1/workout-plans/active` → bài đó trả `sets` đúng thứ tự 12/10/8/6.

## Validate snapshot (US2)

1. `POST /api/v1/workout-sessions` (planId của lộ trình active, vào đúng ngày có bài).
2. `GET /api/v1/workout-sessions/active` → `exercises[].sets` copy đúng 12/10/8/6.
3. `POST /api/v1/workout-sessions/{id}/sets` ghi hiệp 1 = 10 reps → response `setNumber=1`, `repsCompleted=10`, target vẫn giữ trong `exercises[].sets`.

## Validate set_type (US3/US4)

1. Ghi hiệp `{setNumber:1, repsCompleted:20, weightUsed:20, setType:"warm_up"}` → response `setType="warm_up"`.
2. Ghi hiệp `{setNumber:2, repsCompleted:8, setType:"drop_set"}` → response `setType="drop_set"`, `restTimeSeconds=0`.
3. Ghi `setType:"superset"` → 400 validation.

## Backend test

`mvn test` — chạy `WorkoutPlanServiceTest` và `TrackingServiceTest` (đã cập nhật cho target từng hiệp + set_type).
