# Implementation Plan: Màn tập trung tối giản cho buổi tập (016)

**Branch**: `016-workout-execution-mode` | **Date**: 2026-08-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/016-workout-execution-mode/spec.md`

## Summary

Thay thế form dài của `WorkoutSession.tsx` (màn Tập hôm nay) bằng màn tập trung tối giản 4 khối: tên bài + thời lượng buổi → GIF 180×180 → "Hiệp X - mục tiêu" → 1 nút HOÀN THÀNH (bài rep) hoặc đồng hồ đếm ngược (bài duration). Sau mỗi hiệp là màn nghỉ overlay lấy `restTimeSeconds` từ DB. Backend chỉ thêm `mediaUrl` vào `SessionExerciseResponse` + endpoint xóa hiệp (undo 60s). Không migration.

## Technical Context

**Language/Version**: TypeScript (strict) + React 18; Java 17 + Spring Boot 3.3

**Primary Dependencies**: React 18, Vite, Axios (web); Spring Data JPA, Flyway, Jakarta Validation (backend) — KHÔNG thêm dependency mới

**Storage**: PostgreSQL 18 (không đổi schema — DB đang v16)

**Testing**: Jest + React Testing Library (web); JUnit 5 + Mockito (backend)

**Target Platform**: Web SPA (responsive, ưu tiên mobile)

**Project Type**: web (SPA) + backend (REST API)

**Performance Goals**: Timer render ≤ 2 tick/giây; không N+1 khi join media (batch theo exerciseId)

**Constraints**: Layout single-column ≤ 560px; media local `exercises-dataset/`; vùng chạm ≥ 44px; không fixed overlay vượt container; double-tap guard; deadline-based timers

**Scale/Scope**: 1 màn hình web + 1 endpoint mới + 1 DTO mở rộng

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc (constitution) | Trạng thái | Ghi chú |
|---|---|---|
| §2 Immutable Tech Stack (React 18/TS strict, Spring Boot 3.3, JPA, Flyway) | ✅ PASS | Không thêm stack/dependency mới |
| §3 Phân lớp Controller → Service → Repository; Bean Validation cho endpoint ghi | ✅ PASS | Endpoint DELETE mới đi qua service + validate ownership |
| §4 Sync LWW; UPSERT (session_id, session_exercise_id, set_number) | ✅ PASS | Không đổi logic record-set; undo là xóa tường minh trong session active |
| §4 Session auto-expire sang ngày mới | ✅ PASS | Kế thừa `requireOwnedActiveSession`; 409 message "hết hạn vì sang ngày mới" (đã fix trong audit) |
| §4 Retention (transaction data không xóa cứng trước hạn chốt) | ✅ PASS | Retention chốt cho bữa ăn; workout_sets chưa có hạn cố định. Undo giới hạn cửa sổ 60s + session active — là sửa lỗi nhập, không phải xóa lịch sử |
| §5 Media hệ thống dùng `exercises-dataset/` local, không external API | ✅ PASS | `mediaUrl` trỏ local media; fallback icon |
| §6 Code quality (Google Java Style, ESLint, hooks, comment "why") | ✅ PASS | Áp dụng như hiện tại |
| §7 Migration lifecycle — không xóa migration, không cần migration mới | ✅ PASS | Không đổi schema |
| §8 Speckit gates (spec/plan/tasks, DTO validation, state transitions, test, OpenAPI) | ✅ PASS | Đủ artifacts; OpenAPI delta trong contracts/ |
| §9 DoD: test ≥ 80% phần code thay đổi, integration happy+error path, build pass | ⏳ Sẽ verify ở implement | Unit test service xóa + component test màn tập trung |

## Project Structure

### Documentation (this feature)

```text
specs/016-workout-execution-mode/
├── plan.md              # File này
├── research.md          # Phase 0 — quyết định thiết kế
├── data-model.md        # Phase 1 — DTO/endpoint delta
├── quickstart.md        # Phase 1 — kịch bản kiểm thử tay
├── contracts/           # Phase 1 — OpenAPI delta
└── tasks.md             # Phase 2 (/speckit-tasks — chưa tạo)
```

### Source Code (repository root)

```text
web/src/
├── pages/training/
│   ├── WorkoutSession.tsx      # Viết lại theo màn tập trung tối giản
│   └── training/               # (nếu tách) ExecutionScreen, RestOverlay,
│                               #   DurationTimer, MiniEditor, ExerciseQueueChips
├── services/trackingApi.ts     # + deleteSet, WorkoutSet.mediaUrl typing
└── components/                 # tái sử dụng Button/Icon hiện có

backend/src/main/java/com/workoutsmart/
├── tracking/
│   ├── controller/TrackingController.java   # + DELETE /{id}/sets/{setId}
│   ├── service/TrackingService.java         # + deleteSet(userId, sessionId, setId)
│   └── dto/SessionExerciseResponse.java     # + mediaUrl
└── profile/repository/WorkoutSetRepository.java  # (dùng findById sẵn có)
```

**Structure Decision**: Giữ nguyên cấu trúc monorepo `web/` + `backend/` hiện tại; UI mới nằm trong `web/src/pages/training/`, endpoint mới đặt ở `TrackingController` (cùng resource workout-sessions).

## Complexity Tracking

Không có vi phạm constitution cần justify.
