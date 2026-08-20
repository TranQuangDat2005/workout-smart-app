# Tasks: Hiệp tập nâng cao

## Phase 1: Setup & Data

- [x] T001 Migration `V13__advanced_sets.sql` (2 bảng target + `workout_sets.set_type`)
- [x] T002 `SetType` enum (normal/warm_up/drop_set + validate)

## Phase 2: Entities & Repositories

- [x] T003 `WorkoutPlanExerciseSet` + `WorkoutPlanExerciseSetRepository`
- [x] T004 `WorkoutSessionExerciseSet` + `WorkoutSessionExerciseSetRepository`
- [x] T005 `WorkoutSet` thêm `setType`

## Phase 3: Backend — Plan (US1)

- [x] T006 `SetTargetRequest` / `SetTargetResponse` DTO
- [x] T007 `PlanExerciseItemRequest` + `sets`; `PlanExerciseResponse` + `sets`
- [x] T008 `WorkoutPlanService.replaceDayExercises` lưu target từng hiệp
- [x] T009 `WorkoutPlanService.toDayResponse` trả target từng hiệp

## Phase 4: Backend — Tracking (US2/US3/US4)

- [x] T010 `RecordSetRequest` + `setType`; `SetResponse` + `setType`; `SessionExerciseResponse` + `sets`; `SyncSetRequest` + `setType`
- [x] T011 `TrackingService.copyDay` snapshot target từng hiệp
- [x] T012 `TrackingService.recordSet` resolve/lưu `setType` (drop_set → rest=0)
- [x] T013 `TrackingService.toResponse` trả target từng hiệp + setType

## Phase 5: Backend — Tests

- [x] T014 Cập nhật `WorkoutPlanServiceTest` (constructor + target từng hiệp)
- [x] T015 Cập nhật `TrackingServiceTest` (constructor + set_type + snapshot)

## Phase 6: Web

- [x] T016 `planApi.ts` + `trackingApi.ts`: type `SetTarget`, `sets`, `setType`
- [x] T017 UI sửa ngày template hiển thị/nhập target từng hiệp (`PlanEditor`)
- [x] T018 UI tracking hiển thị target vs thực tế + chọn warm-up/drop-set

## Phase 7: Bổ sung theo yêu cầu UIUX

- [x] T019 Tiến bộ 30 ngày (tạ/rep tăng-giảm) — backend `GET /workout-sessions/progress` + bảng chỉ số ở `WorkoutHistoryPage`
- [x] T020 Định nghĩa kiểu set trong `GoalSetupForm` (cạnh tạo lịch tập tự động)

## Dependencies & Execution Order

T001 → T002 → (T003, T004, T005 song song) → (T006..T009) → (T010..T013) → (T014, T015) → (T016..T018) → (T019, T020)
