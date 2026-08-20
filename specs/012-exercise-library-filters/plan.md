# Implementation Plan: Lọc thư viện bài tập theo Category & Equipment

**Branch**: `feature/010-uiux-polish` (spec `012-exercise-library-filters`) | **Date**: 2026-08-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/012-exercise-library-filters/spec.md`

## Summary

Thay bộ lọc thư viện (6 nhóm cơ + 5 dụng cụ, chọn một) bằng Category (10) + Equipment (28) đa chọn, OR trong chiều / AND giữa hai chiều, kèm search tên. Backend `GET /api/v1/exercises` nhận nhiều `category` và `equipment`. Form bài custom bắt buộc Category + Equipment đầy đủ + nhóm cơ 6 giá trị. Không đụng Rule Engine / Goal Setup / builder kéo-thả.

## Technical Context

**Language/Version**: Java 17, TypeScript 5 (strict)

**Primary Dependencies**: Spring Boot 3.3, Spring Data JPA, React 18, Vite, Axios

**Storage**: PostgreSQL 18 — reuse bảng `exercises` (không migration)

**Testing**: JUnit 5 + Mockito + MockMvc; Jest + React Testing Library

**Target Platform**: Web SPA + REST API

**Project Type**: Web application (backend + web)

**Performance Goals**: Kết quả lọc hiện trong 3 giây (SC-003); phân trang size mặc định 20

**Constraints**: Constitution — controller mỏng, logic ở service, EARS đã có trong spec; không xóa migration; không AI/ML

**Scale/Scope**: ~1324 bài hệ thống + bài custom của user; 1 màn thư viện + modal form

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Immutable stack: PASS (không thêm framework)
- Layering Controller → Service → Repository: PASS
- FR EARS tiếng Việt: PASS (spec)
- Domain invariants Rule Engine 5 equipment: PASS (không đổi Goal Setup)
- Test coverage 80% module mới / thay đổi service: PASS (mở rộng test search)
- Không xóa migration: PASS (không migration)

## Project Structure

### Documentation (this feature)

```text
specs/012-exercise-library-filters/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/openapi.yaml
├── spec.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/workoutsmart/exercise/
  controller/ExerciseController.java
  service/ExerciseService.java
  dto/CreateCustomExerciseRequest.java
web/src/pages/plan/ExerciseSearchPage.tsx
web/src/services/planApi.ts
web/src/services/labels.ts
backend/src/test/java/com/workoutsmart/exercise/
  service/ExerciseServiceTest.java
  controller/ExerciseControllerIntegrationTest.java
```

**Structure Decision**: Mở rộng module `exercise` và trang thư viện hiện có; không tạo package mới.

## Complexity Tracking

Không có vi phạm constitution.
