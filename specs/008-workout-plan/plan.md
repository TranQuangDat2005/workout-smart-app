# Implementation Plan: Thiết lập mục tiêu & Tạo lộ trình tập (008-workout-plan)

**Branch**: `feature/008-workout-plan` | **Date**: 2026-08-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/008-workout-plan/spec.md`

## Summary

Feature 008-workout-plan bao gồm 3 User Stories: (1) Thiết lập mục tiêu cá nhân — người dùng chọn goal_type, fitness_level, equipment; (2) Tạo lộ trình tập tự động bằng Rule Engine v1 — map goal_type × fitness_level × equipment thành workout plan với các ngày tập và bài tập cụ thể; (3) Xem chi tiết bài tập — hiển thị GIF, ảnh tĩnh 180×180, hướng dẫn từng bước. Khi bài tập bị Admin ẩn, hệ thống tự động clone vào draft queue, gợi ý thay thế, và thông báo cho người dùng.

## Technical Context

**Language/Version**: Java 17, Spring Boot 3.3, Maven
**Primary Dependencies**: Spring Data JPA, Spring Validation (Jakarta), Lombok, MapStruct, Flyway
**Storage**: PostgreSQL 18 — Exercise library (1324+ rows), workout_plans, workout_plan_days, workout_plan_exercises
**Testing**: JUnit 5 + Mockito (service), MockMvc (controller integration), coverage ≥ 80%
**Target Platform**: REST API backend (web/mobile clients gọi qua HTTP)
**Project Type**: Web service (REST API, không MVC)
**Performance Goals**: Plan generation < 5s; Exercise detail API < 300ms P95
**Constraints**: Rule Engine v1 chỉ dùng rule-based (không AI/ML); bài tập chỉ từ exercise library có sẵn; exercise media từ `exercises-dataset/` local
**Scale/Scope**: 1324+ exercises trong library; mỗi user 1 plan active tại thời điểm; plan sinh tức thì

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc constitution | Status | Ghi chú |
|---|---|---|
| §2 Immutable Tech Stack: Spring Boot 3.3 + Java 17 + PostgreSQL 18 | ✅ PASS | Khớp stack đã chốt |
| §3 Architecture: Controller → Service → Repository → Entity | ✅ PASS | Plan tuân thủ phân lớp |
| §3 Bean Validation cho mọi endpoint ghi | ✅ PASS | DTOs sẽ có @Valid |
| §4 Rule Engine v1: goal_type × fitness_level × equipment, KHÔNG AI/ML | ✅ PASS | Rule engine chỉ dùng bảng luật |
| §4 Workout session auto-expire khi sang ngày mới | ⚠️ N/A | Feature 008 không quản lý session (thuộc 009) |
| §4 Bài ẩn → biến mất khỏi Rule Engine + search; clone draft queue | ✅ PASS | FR-011 cover đầy đủ |
| §5 Audit log cho Admin actions | ⚠️ N/A | Không có admin action trong 008 |
| §6 Google Java Style, constructor injection | ✅ PASS | |
| §7 Migration lifecycle: không xóa, không sửa migration đã chạy | ✅ PASS | Migration mới cho plan tables |
| §8 Speckit gates: UC, entity, DTO validation, auth rules, state transitions, tests | ✅ PASS | Đầy đủ trong plan |

## Project Structure

### Documentation (this feature)

```text
specs/008-workout-plan/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── openapi.yaml
├── checklists/
│   └── requirements.md
├── spec.md              # Feature specification
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/workoutsmart/
│   ├── exercise/
│   │   ├── entity/Exercise.java
│   │   ├── repository/ExerciseRepository.java
│   │   ├── service/ExerciseService.java
│   │   ├── controller/ExerciseController.java
│   │   └── dto/ExerciseDetailResponse.java, ExerciseSearchResponse.java
│   ├── plan/
│   │   ├── entity/WorkoutPlan.java, WorkoutPlanDay.java,
│   │   │        WorkoutPlanExercise.java, DraftExercise.java
│   │   ├── repository/WorkoutPlanRepository.java, WorkoutPlanDayRepository.java,
│   │   │            WorkoutPlanExerciseRepository.java, DraftExerciseRepository.java
│   │   ├── service/RuleEngineService.java, WorkoutPlanService.java, DraftExerciseService.java
│   │   ├── controller/WorkoutPlanController.java
│   │   └── dto/GoalSetupRequest.java, GoalSetupResponse.java, GeneratePlanRequest.java,
│   │            WorkoutPlanResponse.java, PlanDayResponse.java, PlanExerciseResponse.java
│   └── WorkoutSmartApplication.java
├── src/main/resources/db/migration/
│   └── V9__workout_plan_feature.sql
└── src/test/java/com/workoutsmart/
    ├── exercise/service/ExerciseServiceTest.java
    ├── exercise/controller/ExerciseControllerIntegrationTest.java
    ├── plan/service/RuleEngineServiceTest.java
    ├── plan/service/WorkoutPlanServiceTest.java
    ├── plan/service/DraftExerciseServiceTest.java
    └── plan/controller/WorkoutPlanControllerIntegrationTest.java
```

**Structure Decision**: Tuân thủ kiến trúc phân lớp Backend-only (REST API) theo constitution §3 và convention package hiện có `com.workoutsmart.<domain>`. Feature này không có frontend code — web/mobile sẽ consume API. Exercise và Plan được triển khai trong package `exercise`/`plan` tương ứng; DraftExercise nằm trong `plan` (gắn chặt với lộ trình).

## Complexity Tracking

Không có vi phạm constitution cần justify.

## Constitution Re-check (post Phase 1 design)

Tất cả gates vẫn PASS sau khi thiết kế data model và contracts. Không có thay đổi nào mâu thuẫn với constitution.
