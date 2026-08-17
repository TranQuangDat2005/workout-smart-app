# Tasks: Thiết lập mục tiêu & Tạo lộ trình tập (008-workout-plan)

**Input**: Design documents from `/specs/008-workout-plan/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: BẮT BUỘC theo constitution §9 (coverage ≥80% service + integration happy/error path).

**Organization**: Tasks grouped by user story. Tất cả 3 US đều P1, có thể implement theo thứ tự US1 → US2 → US3 hoặc song song nếu đủ nhân lực.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Có thể chạy song song (file khác nhau, không phụ thuộc nhau)
- **[Story]**: US nào (US1, US2, US3)
- Đính kèm file path chính xác trong mô tả

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Cấu trúc project, migration, cấu hình chung

- [ ] T001 Create Flyway migration `V3__workout_plan_tables.sql` với 4 bảng mới: `workout_plans`, `workout_plan_days`, `workout_plan_exercises`, `draft_exercises` + indexes theo data-model.md §7 tại `backend/src/main/resources/db/migration/V3__workout_plan_tables.sql`
- [ ] T002 [P] Create `GoalSetupRequest` DTO với validation annotations (goalType enum, fitnessLevel enum, equipment array minSize=1, effectiveDate nullable) tại `backend/src/main/java/com/workoutsmart/dto/GoalSetupRequest.java`
- [ ] T003 [P] Create `GoalSetupResponse` DTO tại `backend/src/main/java/com/workoutsmart/dto/GoalSetupResponse.java`
- [ ] T004 [P] Create `WorkoutPlanResponse` DTO tại `backend/src/main/java/com/workoutsmart/dto/WorkoutPlanResponse.java`
- [ ] T005 [P] Create `PlanDayResponse` DTO tại `backend/src/main/java/com/workoutsmart/dto/PlanDayResponse.java`
- [ ] T006 [P] Create `PlanExerciseResponse` DTO tại `backend/src/main/java/com/workoutsmart/dto/PlanExerciseResponse.java`
- [ ] T007 [P] Create `ExerciseDetailResponse` DTO tại `backend/src/main/java/com/workoutsmart/dto/ExerciseDetailResponse.java`
- [ ] T008 [P] Create `ExerciseSearchRequest` DTO (equipment, category, bodyPart, muscleGroup, q, page, size) tại `backend/src/main/java/com/workoutsmart/dto/ExerciseSearchRequest.java`
- [ ] T009 [P] Create `ExerciseSearchResponse` DTO tại `backend/src/main/java/com/workoutsmart/dto/ExerciseSearchResponse.java`

---

## Phase 2: Foundational — Exercise Domain (Blocking)

**Purpose**: Exercise entity, repository, service — nền tảng cho TẤT CẢ user stories

**⚠ CRITICAL**: Không thể bắt đầu US1/US2/US3 nếu phase này chưa xong

- [ ] T010 [P] Create `Exercise` entity (JPA) với tất cả fields theo data-model.md §1, `@Entity`, `@Table(name="exercises")`, `@Enumerated` cho status, KHÔNG dùng `@Data` (Lombok) vì có lazy relationships tiềm ẩn tại `backend/src/main/java/com/workoutsmart/domain/exercise/Exercise.java`
- [ ] T011 [P] Create `ExerciseRepository` extends `JpaRepository<Exercise, Long>` với custom query: `findByStatus(String status)`, `findByEquipmentInAndStatus(List<String> equipment, String status)` tại `backend/src/main/java/com/workoutsmart/domain/exercise/ExerciseRepository.java`
- [ ] T012 Create `ExerciseService` với method `findActiveByEquipment(List<String> equipment)` — truy vấn bài active theo dụng cụ người dùng (dùng bởi Rule Engine tại US1) tại `backend/src/main/java/com/workoutsmart/domain/exercise/ExerciseService.java`
- [ ] T013 Run `mvn test` để verify Exercise domain compile + chạy không lỗi tại `backend/`

**Checkpoint**: Exercise domain sẵn sàng — US1/US2/US3 có thể bắt đầu

---

## Phase 3: User Story 1 — Thiết lập mục tiêu cá nhân (Priority: P1) 🎯 MVP

**Goal**: Người dùng chọn goal_type, fitness_level, equipment → lưu vào profile → trigger plan generation

**Independent Test**: Gọi `PUT /api/v1/users/me/goals` với `{goalType: "weight_loss", fitnessLevel: "beginner", equipment: ["body_weight"]}` → expect 200 + plan được tạo trong DB

### Tests for User Story 1 ⚠️

> **NOTE: Viết test TRƯỚC, chạy FAIL trước khi implement**

- [ ] T014 [P] [US1] Unit test `RuleEngineService` tại `backend/src/test/java/com/workoutsmart/domain/workoutplan/RuleEngineServiceTest.java` — verify bảng luật goal_type × fitness_level × equipment (FR-008/009/010), số ngày, reps, rest time, sets, lọc equipment
- [ ] T015 [P] [US1] Unit test `WorkoutPlanService` tại `backend/src/test/java/com/workoutsmart/domain/workoutplan/WorkoutPlanServiceTest.java` — verify generatePlan archive plan cũ, getActivePlan, archivePlan
- [ ] T016 [US1] Integration test `WorkoutPlanController` tại `backend/src/test/java/com/workoutsmart/domain/workoutplan/WorkoutPlanControllerTest.java` — PUT goals happy path + 400 validation + 422 no matching exercise

### Implementation for User Story 1

- [ ] T017 [US1] Create `WorkoutPlan` entity (JPA) với fields: id, userId, name, goalType, fitnessLevel, status, createdAt, updatedAt; `@ManyToOne` relationship với User (từ 007) tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlan.java`
- [ ] T018 [US1] Create `WorkoutPlanDay` entity (JPA) với fields: id, planId, dayOfWeek, createdAt; `@ManyToOne` với WorkoutPlan tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanDay.java`
- [ ] T019 [US1] Create `WorkoutPlanExercise` entity (JPA) với fields: id, dayId, exerciseId, targetSets, targetReps, restTimeSeconds, createdAt; `@ManyToOne` với WorkoutPlanDay và Exercise tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanExercise.java`
- [ ] T020 [P] [US1] Create `WorkoutPlanRepository` extends `JpaRepository<WorkoutPlan, Long>` với query: `findByUserIdAndStatus(Long userId, String status)`, `countByUserIdAndStatus(Long userId, String status)` tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanRepository.java`
- [ ] T021 [P] [US1] Create `WorkoutPlanDayRepository` extends `JpaRepository<WorkoutPlanDay, Long>` với query: `findByPlanId(Long planId)` tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanDayRepository.java`
- [ ] T022 [P] [US1] Create `WorkoutPlanExerciseRepository` extends `JpaRepository<WorkoutPlanExercise, Long>` với query: `findByDayId(Long dayId)` tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanExerciseRepository.java`
- [ ] T023 [US1] Create `RuleEngineService` với method `generatePlan(User user)` : đọc goal_type/fitness_level/equipment từ User → apply bảng luật Rule Engine v1 (FR-008, FR-009, FR-010) → tạo WorkoutPlan + WorkoutPlanDays + WorkoutPlanExercises. Logic: goal_type → số ngày/tuần + reps + rest_time; fitness_level → số bài/ngày + sets; equipment → filter exercises từ ExerciseService. Trả về WorkoutPlan entity tree tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/RuleEngineService.java`
- [ ] T024 [US1] Create `WorkoutPlanService` với methods: `getActivePlan(Long userId)`, `generatePlan(Long userId, LocalDate effectiveDate)`, `archivePlan(Long planId)`. Method `generatePlan`: archive plan active hiện tại (nếu có) → gọi RuleEngineService → lưu plan mới. Method `getActivePlan`: query plan active + eager load days + exercises tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanService.java`
- [ ] T025 [US1] Create `WorkoutPlanController` với endpoint: `PUT /api/v1/users/me/goals` — nhận `GoalSetupRequest` → validate → cập nhật User profile (goal_type, fitness_level, equipment) → gọi `WorkoutPlanService.generatePlan()` → trả `GoalSetupResponse` với planId + warning (nếu archive plan cũ). HTTP 400 nếu validation fail, 422 nếu không có exercise khớp tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanController.java`

**Checkpoint**: US1 hoàn tất — user có thể thiết lập mục tiêu và plan được tạo tự động

---

## Phase 4: User Story 2 — Tạo lộ trình tập & Xem plan (Priority: P1)

**Goal**: User xem workout plan active, xem lịch trình tuần, system xử lý goal change với 2 options

**Independent Test**: Gọi `GET /api/v1/workout-plans/active` → trả plan với days + exercises. Thay đổi goal → có warning + 2 options.

### Tests for User Story 2 ⚠️

- [ ] T026 [US2] Integration test `WorkoutPlanController` mở rộng tại `backend/src/test/java/com/workoutsmart/domain/workoutplan/WorkoutPlanControllerTest.java` — GET active (happy + 404), POST generate (happy + effectiveDate today/tomorrow + 400/422)

### Implementation for User Story 2

- [ ] T027 [US2] Add `GET /api/v1/workout-plans/active` endpoint to `WorkoutPlanController` — query plan active của user → map entity → `WorkoutPlanResponse` (plan → days → exercises). HTTP 404 nếu chưa có plan tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanController.java`
- [ ] T028 [US2] Add `POST /api/v1/workout-plans/generate` endpoint to `WorkoutPlanController` — trigger thủ công `WorkoutPlanService.generatePlan()` với effectiveDate optional. Dùng khi user muốn refresh plan tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanController.java`
- [ ] T029 [US2] Implement `effectiveDate` logic trong `WorkoutPlanService.generatePlan()`: nếu effectiveDate = today → archive immediately; nếu effectiveDate = tomorrow → plan mới chỉ active từ ngày mai (plan cũ giữ active đến hết hôm nay). Dùng `LocalDate.now()` + timezone system tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/WorkoutPlanService.java`
- [ ] T030 [US2] Implement exercise exclusion logic trong `RuleEngineService`: query exercises WHERE status='active' AND equipment IN (user's equipment). FR-006: bài inactive không xuất hiện trong plan mới tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/RuleEngineService.java`

**Checkpoint**: US2 hoàn tất — user xem được plan, goal change hoạt động đúng

---

## Phase 5: User Story 3 — Xem chi tiết bài tập & Tìm kiếm (Priority: P1)

**Goal**: User xem chi tiết bài tập (GIF, ảnh, instructions), tìm kiếm theo filter

**Independent Test**: Gọi `GET /api/v1/exercises/{id}` → trả detail. `GET /api/v1/exercises?equipment=body_weight` → trả danh sách filtered.

### Tests for User Story 3 ⚠️

- [ ] T031 [P] [US3] Unit test `ExerciseService` tại `backend/src/test/java/com/workoutsmart/domain/exercise/ExerciseServiceTest.java` — search filter + pagination + findById (404 khi không tồn tại)
- [ ] T032 [US3] Integration test `ExerciseController` tại `backend/src/test/java/com/workoutsmart/domain/exercise/ExerciseControllerTest.java` — GET /exercises (filter + pagination) + GET /exercises/{id} (happy + 404)

### Implementation for User Story 3

- [ ] T033 [US3] Implement `ExerciseService.search()` method: build động query với Specifications hoặc `@Query` method filter theo equipment/category/bodyPart/muscleGroup/q + pagination (page/size). Trả `ExerciseSearchResponse` (content + totalElements + totalPages + page) tại `backend/src/main/java/com/workoutsmart/domain/exercise/ExerciseService.java`
- [ ] T034 [US3] Implement `ExerciseService.findById()` method: query exercise theo ID, throw `NotFoundException` (404) nếu không tồn tại. Trả `ExerciseDetailResponse` với full fields (name, category, bodyPart, equipment, target, muscleGroup, image, gifUrl, instructions) tại `backend/src/main/java/com/workoutsmart/domain/exercise/ExerciseService.java`
- [ ] T035 [US3] Create `ExerciseController` với endpoints: `GET /api/v1/exercises` (search), `GET /api/v1/exercises/{exerciseId}` (detail) — theo contracts/openapi.yaml paths `/exercises` và `/exercises/{exerciseId}` tại `backend/src/main/java/com/workoutsmart/domain/exercise/ExerciseController.java`

**Checkpoint**: US3 hoàn tất — exercise search + detail hoạt động

---

## Phase 6: Draft Queue — Hidden Exercise Handling (Edge Case)

**Goal**: Khi bài tập trong active plan bị Admin ẩn → clone vào draft queue + gợi ý thay thế

**Independent Test**: Admin ẩn exercise trong active plan → system tạo draft_exercises row + replacement_suggestion.

### Tests

- [ ] T036 [P] Unit test `DraftExerciseService` tại `backend/src/test/java/com/workoutsmart/domain/draftqueue/DraftExerciseServiceTest.java` — handleExerciseHidden tạo clone + replacement, cleanupBySession xóa đúng session

### Implementation

- [ ] T037 [P] Create `DraftExercise` entity (JPA) với fields: id, sessionId, originalExerciseId, clonedExerciseId, replacementExerciseId, createdAt; `@ManyToOne` relationships tại `backend/src/main/java/com/workoutsmart/domain/draftqueue/DraftExercise.java`
- [ ] T038 [P] Create `DraftExerciseRepository` extends `JpaRepository<DraftExercise, Long>` với query: `findBySessionId(Long sessionId)`, `deleteBySessionId(Long sessionId)` tại `backend/src/main/java/com/workoutsmart/domain/draftqueue/DraftExerciseRepository.java`
- [ ] T039 Create `DraftExerciseService` với methods: `handleExerciseHidden(Long exerciseId)` — find all active plans containing this exercise → for each: create draft clone + find replacement (same muscle_group + body_part, ORDER BY RANDOM() LIMIT 1) → return list of affected users; `cleanupBySession(Long sessionId)` — delete all draft rows for completed session tại `backend/src/main/java/com/workoutsmart/domain/draftqueue/DraftExerciseService.java`

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Hoàn thiện, edge cases, validation, documentation

- [ ] T040 [P] Add Bean Validation (`@Valid`) on all request DTOs and `@NotNull`/`@NotBlank` on required fields. Verify GlobalExceptionHandler (từ 007) xử lý 400/422 đúng tại `backend/src/main/java/com/workoutsmart/dto/`
- [ ] T041 [P] Handle edge cases: "không có bài tập nào khớp" → 422 + message rõ ràng; "kho bài tập trống" → 422; "tất cả bài bị ẩn" → 422. Add trong `RuleEngineService.generatePlan()` tại `backend/src/main/java/com/workoutsmart/domain/workoutplan/RuleEngineService.java`
- [ ] T042 [P] Verify OpenAPI spec (`contracts/openapi.yaml`) khớp với controller implementations. Update nếu có endpoint mới/sửa tại `specs/008-workout-plan/contracts/openapi.yaml`
- [ ] T043 Run quickstart.md validation scenarios: test từng endpoint theo quickstart.md §3 tại `backend/`
- [ ] T044 [P] Run `mvn test` + jacoco để verify coverage ≥80% cho phần workout plan/exercise/draft queue. Sửa test nếu thiếu coverage tại `backend/`
- [ ] T045 Update `specs/008-workout-plan/spec.md` status từ Draft thành Ready (nếu tất cả acceptance scenarios đã cover) tại `specs/008-workout-plan/spec.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Không phụ thuộc — bắt đầu ngay
- **Foundational (Phase 2)**: Phụ thuộc Setup (migration + DTOs) — BLOCKS tất cả user stories
- **US1 (Phase 3)**: Phụ thuộc Foundational — tạo entities + Rule Engine + goal setup endpoint
- **US2 (Phase 4)**: Phụ thuộc US1 (cùng entities + services) — thêm endpoints + logic
- **US3 (Phase 5)**: Phụ thuộc Foundational — song song được với US1/US2
- **Draft Queue (Phase 6)**: Phụ thuộc US1 (cần WorkoutPlan entities) — có thể chạy sau US2
- **Polish (Phase 7)**: Phụ thuộc tất cả phases trước

### User Story Dependencies

- **US1 (P1)**: Cần Phase 2 xong. Không phụ thuộc US2/US3
- **US2 (P1)**: Cần US1 xong (cùng entities/services). Không phụ thuộc US3
- **US3 (P1)**: Cần Phase 2 xong. Không phụ thuộc US1/US2 — **chạy song song được**

### Within Each User Story

- Tests viết TRƯỚC, chạy FAIL trước khi implement
- Entities → Repositories → Services → Endpoints
- Story hoàn thành trước khi chuyển sang story tiếp theo

### Parallel Opportunities

```
Phase 1 (Setup):  T002-T009 chạy song song (DTOs independence)
Phase 2:          T010-T011 chạy song song (Entity + Repository)
Phase 3:          T014-T015 chạy song song (2 unit tests)
                  T017-T019 chạy song song (3 entities)
                  T020-T022 chạy song song (3 repositories)
Phase 5:          T031 chạy song song với US1/US2
Phase 6:          T036 chạy song song (unit test), T037-T038 chạy song song (Entity + Repository)
Phase 7:          T040-T042 chạy song song
```

### Cross-Story Parallel Strategy

```
Foundational (Phase 2) xong
  ├── US1 (Phase 3) ← Developer A
  ├── US3 (Phase 5) ← Developer B (song song với US1)
  └── US2 (Phase 4) ← Developer A (sau US1)
      └── Draft Queue (Phase 6) ← Developer A (sau US2)
```

---

## Implementation Strategy

### MVP First (US1 + US2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: US1 (Goal Setup + Plan Generation)
4. Complete Phase 4: US2 (View Plan + Goal Change)
5. **STOP and VALIDATE**: User có thể set goal → system tạo plan → user xem plan
6. Deploy/demo nếu ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 + US2 → Test independently → Deploy/Demo (MVP!)
3. Add US3 → Exercise search + detail → Deploy/Demo
4. Add Draft Queue → Edge case handling → Deploy/Demo
5. Polish → Validation + cleanup → Final release

---

## Notes

- [P] tasks = file khác nhau, không phụ thuộc — chạy song song an toàn
- [Story] label map task đến user story để truy vết
- Mỗi user story independently completable và testable
- Test coverage ≥80% bắt buộc theo constitution §9
- Commit sau mỗi task hoặc logical group
- Dừng tại checkpoint để validate story independently
