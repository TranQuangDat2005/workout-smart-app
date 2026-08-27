# Tasks: Thống kê & Báo cáo Tiến độ (005-stats-reports)

## Phase 1: Setup

- [x] T001 Tạo nhánh `feature/005-stats-reports` từ develop (`git flow feature start 005-stats-reports`)

## Phase 2: Foundational (bắt buộc trước mọi user story)

- [x] T002 Tạo entity `WorkoutPlan`, `WorkoutPlanDay`, `WorkoutPlanExercise` trong `backend/src/main/java/com/workoutsmart/plan/entity/` (map bảng V5)
- [x] T003 Tạo repository `WorkoutPlanRepository`, `WorkoutPlanDayRepository`, `WorkoutPlanExerciseRepository` trong `backend/src/main/java/com/workoutsmart/plan/repository/`
- [x] T004 [P] Thêm derived query vào `WorkoutSessionRepository` (`backend/src/main/java/com/workoutsmart/profile/repository/WorkoutSessionRepository.java`)
- [x] T005 [P] Thêm derived query vào `WorkoutSetRepository` (`backend/src/main/java/com/workoutsmart/profile/repository/WorkoutSetRepository.java`)
- [x] T006 [P] Thêm derived query vào `BodyMetricRepository` (`backend/src/main/java/com/workoutsmart/nutrition/repository/BodyMetricRepository.java`)
- [x] T007 [P] Thêm derived query vào `MealLogRepository` + `MealEntryRepository` + `MealDailySummaryRepository` (`backend/src/main/java/com/workoutsmart/nutrition/repository/`)
- [x] T008 [P] Tạo DTO `WeightPoint`, `VolumePoint`, `StreakStats`, `PlanCompletion`, `CaloriePoint`, `StatsDashboardResponse` trong `backend/src/main/java/com/workoutsmart/stats/dto/`

## Phase 3: User Story 1 — Dashboard tổng quan (P1)

- [x] T009 [US1] Tạo `StatsService` trong `backend/src/main/java/com/workoutsmart/stats/service/StatsService.java` (weight, volume, streak qua `StreakCalculator`, plan completion, calo in/out)
- [x] T010 [US1] Tạo `StatsController` trong `backend/src/main/java/com/workoutsmart/stats/controller/StatsController.java` — `GET /api/v1/stats/dashboard` (from/to optional, validate range ≤366 ngày, lỗi 422)
- [x] T011 [US1] Viết unit test `backend/src/test/java/com/workoutsmart/stats/service/StatsServiceTest.java` (Mockito, ≥80% coverage service)
- [x] T012 [US1] Viết integration test `backend/src/test/java/com/workoutsmart/stats/controller/StatsControllerIntegrationTest.java` (seed dữ liệu, happy + empty + range lỗi)
- [x] T013 [US1] Tạo `web/src/services/statsApi.ts` (typed client `getDashboard`)

## Phase 4: User Story 2 — Lọc theo khoảng thời gian (P2)

- [x] T014 [US2] Tạo `web/src/pages/stats/StatsPage.tsx`: filter 7/30/90/custom, biểu đồ SVG (line cân nặng, bar volume, bar calo), card streak + progress plan, empty state hướng dẫn
- [x] T015 [US2] Nối route `/stats` vào `web/src/App.tsx`

## Clarifications applied

- [x] Tách trạng thái session `completed` khỏi target attainment theo từng set.

## Phase 5: Polish

- [ ] T016 Chạy `mvn test` (backend), `npm run build` + `npm run lint` (web)
- [ ] T017 Commit Conventional Commits + `git flow feature finish 005-stats-reports`

## Dependencies

- US1 → US2 (trang web cần API hoàn chỉnh)
- T002/T003 → T009 (service cần plan entity)
- T004–T007 → T009 (service cần query)

## Parallel

- T004–T008 chạy song song (khác file)
- T011 và T013 chạy song song sau T009/T010
