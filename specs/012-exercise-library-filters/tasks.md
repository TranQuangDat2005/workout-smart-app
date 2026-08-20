# Tasks: Lọc thư viện bài tập theo Category & Equipment

**Input**: Design documents from `/specs/012-exercise-library-filters/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Phase 1: Setup

- [x] T001 Spec + plan artifacts tại `specs/012-exercise-library-filters/`

---

## Phase 2: Foundational

- [x] T002 [P] Shared taxonomy + nhãn Việt tại `web/src/services/labels.ts`
- [x] T003 [P] Cập nhật `specs/008-workout-plan/spec.md` FR-013 (supersede UI thư viện)

---

## Phase 3: User Story 1+2 — Filter & search (P1) 🎯 MVP

- [x] T004 [US1] Unit test multi-value search tại `backend/src/test/java/com/workoutsmart/exercise/service/ExerciseServiceTest.java`
- [x] T005 [US1] Integration test OR/AND query params tại `backend/src/test/java/com/workoutsmart/exercise/controller/ExerciseControllerIntegrationTest.java`
- [x] T006 [US1] `ExerciseService` + `ExerciseController` nhận `List<String>` category/equipment, `IN` predicate
- [x] T007 [US1] `planApi.searchExercises` gửi repeated query params tại `web/src/services/planApi.ts`
- [x] T008 [US1] UI chip đa chọn Category/Equipment, bỏ nhóm cơ, search tại `web/src/pages/plan/ExerciseSearchPage.tsx`

---

## Phase 4: User Story 3 — Form custom taxonomy (P2)

- [x] T009 [US3] Bean Validation category/equipment/muscleGroup tại `CreateCustomExerciseRequest.java` (và update DTO nếu cần)
- [x] T010 [US3] Form tạo/sửa bài: 10 category + 28 equipment + 6 nhóm cơ; `bodyPart = category` tại `ExerciseSearchPage.tsx` + `ExerciseService.createCustomExercise`

---

## Phase 5: Polish

- [x] T011 Chạy `mvn test` (exercise module) và kiểm tra lint trang thư viện

## Dependencies

- T002/T003 song song → T004–T008 → T009–T010 → T011
