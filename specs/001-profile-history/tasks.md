# Tasks: Hồ sơ & Lịch sử tập (001-profile-history)

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Backend

- [x] T001 Entity `WorkoutSession` + `WorkoutSet` tại `backend/src/main/java/com/workoutsmart/profile/entity/`
- [x] T002 Repository `WorkoutSessionRepository`, `WorkoutSetRepository` tại `backend/src/main/java/com/workoutsmart/profile/repository/`
- [x] T003 [US1] DTO ProfileResponse, UpdateProfileRequest/Response, MessageResponse
- [x] T004 [US2] DTO WorkoutSessionResponse, WorkoutSessionDetailResponse, WorkoutSetResponse
- [x] T005 [US1] `ProfileService` (get/update/delete account) tại `backend/src/main/java/com/workoutsmart/profile/service/ProfileService.java`
- [x] T006 [US2] `ProfileService` (sessions phân trang + detail) — cùng file
- [x] T007 `ProfileController` tại `backend/src/main/java/com/workoutsmart/profile/controller/ProfileController.java`

## Phase 2: Tests

- [x] T008 [US1][US2] Unit test `ProfileServiceTest` (9 cases)
- [x] T009 [US1][US2] Integration test `ProfileControllerIntegrationTest` (8 cases: profile, update, validation, delete account, sessions, 404, auth)

## Phase 3: Web

- [x] T010 [US1] `web/src/services/profileApi.ts`
- [x] T011 [US1] `web/src/pages/profile/ProfilePage.tsx`
- [x] T012 [US2] `web/src/pages/profile/WorkoutHistoryPage.tsx`

## Phase 4: Docs

- [x] T013 `plan.md`, `data-model.md`, `contracts/openapi.yaml`, `tasks.md`

## DoD

- [ ] `mvn test` pass — coverage profile ≥80%
- [ ] `npm run build` + `npm run lint` pass
- [ ] `git flow feature finish 001-profile-history`

## Clarifications applied

- [x] Early-completed sessions retain actual workout data in history.
- [x] Destructive history confirmation uses accessible Modal in the Web UI.
