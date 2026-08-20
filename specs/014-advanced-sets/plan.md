# Implementation Plan: Hiệp tập nâng cao

**Branch**: `014-advanced-sets` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/014-advanced-sets/spec.md`

## Summary

Thêm target reps/tạ riêng cho từng hiệp (mở khóa pyramid/flex khi lập kế hoạch) và kiểu set `normal`/`warm_up`/`drop_set`. Migration additive `V13`, không thêm endpoint mới — chỉ mở rộng payload `replaceDayExercises` (`sets[]`), snapshot (`sets[]`) và record set (`set_type`).

## Technical Context

**Language/Version**: Java 17, TypeScript (React 18)

**Primary Dependencies**: Spring Boot 3.3 + Spring Data JPA, Vite SPA

**Storage**: PostgreSQL 18 / H2 test, Flyway (thêm `V13__advanced_sets.sql`)

**Testing**: JUnit 5 + Mockito (backend), Jest + React Testing Library (web)

**Target Platform**: Web SPA

**Project Type**: Web application (React SPA + Spring REST)

**Performance Goals**: Snapshot copy target từng hiệp không thay đổi độ phức tạp (vẫn O(số bài × số hiệp))

**Constraints**: Không đổi invariant sync `(session_id, session_exercise_id, set_number)`; `set_type` chỉ 3 giá trị; không xóa migration cũ

**Scale/Scope**: 2 bảng mới + 1 cột; payload 3 endpoint mở rộng; UI sửa ngày + màn tracking

## Constitution Check

PASS — migration additive (không xóa/sửa migration cũ); không đổi invariant streak/TDEE/sync; FR viết EARS tiếng Việt.

Post-design: PASS — `workout_sets` giữ nguyên key UPSERT; chỉ thêm `set_type` (không phá Last-Write-Wins).

## Project Structure

### Documentation (this feature)

```text
specs/014-advanced-sets/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/openapi.yaml
├── checklists/requirements.md
└── tasks.md
```

### Source Code

```text
backend/src/main/resources/db/migration/V13__advanced_sets.sql
backend/src/main/java/com/workoutsmart/common/SetType.java
backend/src/main/java/com/workoutsmart/plan/entity/WorkoutPlanExerciseSet.java
backend/src/main/java/com/workoutsmart/tracking/entity/WorkoutSessionExerciseSet.java
backend/src/main/java/com/workoutsmart/plan/repository/WorkoutPlanExerciseSetRepository.java
backend/src/main/java/com/workoutsmart/tracking/repository/WorkoutSessionExerciseSetRepository.java
backend/src/main/java/com/workoutsmart/plan/dto/SetTargetRequest.java
backend/src/main/java/com/workoutsmart/plan/dto/SetTargetResponse.java
backend/src/main/java/com/workoutsmart/plan/service/WorkoutPlanService.java   # sửa
backend/src/main/java/com/workoutsmart/tracking/service/TrackingService.java # sửa
backend/src/main/java/com/workoutsmart/profile/entity/WorkoutSet.java       # sửa
backend/src/main/java/com/workoutsmart/plan/dto/{PlanExerciseItemRequest,PlanExerciseResponse}.java # sửa
backend/src/main/java/com/workoutsmart/tracking/dto/{RecordSetRequest,SetResponse,SessionExerciseResponse,SyncSetRequest}.java # sửa

web/src/services/planApi.ts      # sửa (SetTarget + sets)
web/src/services/trackingApi.ts  # sửa (SetTarget + setType)
```

## Phase 0 / Phase 1

Xem [research.md](./research.md), [data-model.md](./data-model.md), [quickstart.md](./quickstart.md). Contract mở rộng tại [contracts/openapi.yaml](./contracts/openapi.yaml).
