# Implementation Plan: Tự xây lộ trình

**Branch**: `feature/010-uiux-polish` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/013-manual-workout-builder/spec.md`

## Summary

Template tuần sửa được trên tab Lịch tập (PUT replace ngày, dnd-kit). Start session copy snapshot `workout_session_exercises`. Sets unique theo (session, session_exercise, set_number).

Picker **Thêm bài** tái sử dụng cùng UX lọc/xem trước của thư viện (012): chip Category + Equipment, tìm tên, thumbnail, GIF/hướng dẫn, nút xác nhận thêm. Không API mới. Tách component web dùng chung để thư viện và picker không lệch nhau.

## Technical Context

**Language/Version**: Java 17, TypeScript (React 18)

**Primary Dependencies**: Spring Boot 3.3, Vite SPA, `@dnd-kit/*`

**Storage**: PostgreSQL 18 / H2 test, Flyway (V12 đã có — picker không migration)

**Testing**: JUnit 5 + Mockito (backend, không đổi); Jest + React Testing Library (picker)

**Target Platform**: Web SPA

**Project Type**: Web application (React SPA + Spring REST)

**Performance Goals**: Danh sách picker cập nhật trong 3 giây (SC-005)

**Constraints**: Không API mới; không chip 6 nhóm cơ; không CRUD bài custom trong picker; tối đa 15 bài/ngày

**Scale/Scope**: 1 màn lịch tập + 1 component dùng chung với thư viện; kho ~1324 bài

## Constitution Check

PASS — không xóa migration; không đổi invariant streak/TDEE/sync; picker chỉ UI + tái sử dụng `GET /exercises` và `GET /exercises/{id}` đã có. FR viết EARS tiếng Việt.

Post-design: PASS — không entity mới, không endpoint mới.

## Project Structure

### Documentation (this feature)

```text
specs/013-manual-workout-builder/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code

```text
web/src/components/ExerciseBrowser.tsx   # lọc + list + xem trước (+ nút Thêm khi picker)
web/src/components/ExerciseBrowser.test.tsx
web/src/components/Modal.tsx             # size lg cho picker
web/src/pages/training/PlanEditor.tsx    # modal Thêm bài dùng ExerciseBrowser
web/src/pages/plan/ExerciseSearchPage.tsx # thư viện dùng ExerciseBrowser
web/src/index.css                        # .modal-dialog.modal-lg
```

## Phase 0 / Phase 1

Xem [research.md](./research.md), [data-model.md](./data-model.md), [quickstart.md](./quickstart.md). Không thêm `contracts/` — tái sử dụng OpenAPI 012 (`GET /exercises`, `GET /exercises/{id}`).
