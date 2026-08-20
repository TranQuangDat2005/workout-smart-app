# Feature Specification: Bài tập tính theo thời gian (Plank, Chạy bộ…)

**Feature Branch**: `015-time-based-exercises`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Bài như plank hay chạy bộ phải tính bằng thời gian, không tính set/reps."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Nhận diện bài tập theo thời gian (Priority: P1)

Mỗi bài tập có một kiểu đo: đo bằng reps/tạ (mặc định) hoặc đo bằng thời gian (plank, cardio, giãn cơ). Hệ thống dùng kiểu đo này để quyết định cách hiển thị và ghi nhận.

**Acceptance Scenarios**:

1. **Given** bài tập có kiểu đo `duration`, **When** hiển thị trong template/buổi tập, **Then** hệ thống hiển thị mục tiêu theo giây (vd 60s), không hiển thị reps.
2. **Given** bài tập có kiểu đo `reps_weight`, **When** hiển thị, **Then** giữ nguyên cách hiện tại (reps × tạ).

---

### User Story 2 - Ghi nhận hiệp theo thời gian (Priority: P1)

Người tập ghi một hiệp plank/chạy bằng thời gian (mm:ss) thay vì số lần và tạ.

**Acceptance Scenarios**:

1. **Given** bài đang tập có kiểu đo `duration`, **When** mở form ghi hiệp, **Then** hệ thống hiển thị ô "Thời gian (mm:ss)" thay cho ô reps và tạ.
2. **Given** User nhập "1:00" và lưu, **Then** hệ thống lưu `duration_seconds = 60`.
3. **Given** bài `reps_weight`, **When** ghi hiệp, **Then** vẫn lưu reps/tạ như cũ, `duration_seconds` rỗng.

---

### User Story 3 - Rule Engine sinh mục tiêu thời gian (Priority: P2)

Khi tạo lộ trình tự động, bài cardio/giãn cơ/isometric (plank, wall sit) được sinh mục tiêu thời gian thay vì reps.

**Acceptance Scenarios**:

1. **Given** goal sinh lộ trình chứa bài cardio, **When** tạo plan, **Then** bài cardio có `target_duration_seconds` (15–20 phút), `target_reps = 0`.
2. **Given** bài plank trong lộ trình, **When** tạo plan, **Then** plank có `target_duration_seconds = 60` mỗi hiệp.

---

### Edge Cases

- Bài `duration` có `target_reps = 0` (sentinel) — không coi 0 là hợp lệ cho reps.
- `measure_type` mặc định `reps_weight` cho bài chưa phân loại / bài tự tạo.
- Phân loại: cardio + stretching tự động là `duration`; isometric (plank, wall sit, hold, bridge, superman) theo tên.
- Khoảng cách (km) cho chạy bộ ngoài scope đợt này.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: THE hệ thống SHALL có trường `measure_type` trên bài tập (`reps_weight` mặc định, `duration` cho bài tính theo thời gian).
- **FR-002**: THE hệ thống SHALL lưu `target_duration_seconds` cho target từng hiệp và target chung của bài `duration`; `target_reps` của bài `duration` SHALL bằng 0.
- **FR-003**: WHEN ghi hiệp bài `duration`, THE hệ thống SHALL lưu `duration_seconds`; `reps_completed`/`weight_used` SHALL rỗng.
- **FR-004**: WHEN hiển thị bài `duration`, client SHALL hiển thị thời gian (mm:ss) thay cho reps/tạ, trong cả template và màn ghi hiệp.
- **FR-005**: WHEN Rule Engine sinh lộ trình, WHERE bài là `duration`, THE hệ thống SHALL sinh `target_duration_seconds` theo goal (cardio 15–20 phút; isometric 60s), `target_reps = 0`.

### Key Entities

- **Exercise**: thêm `measure_type`.
- **WorkoutPlanExercise / WorkoutSessionExercise**: thêm `target_duration_seconds` (và `measure_type` cho snapshot).
- **WorkoutPlanExerciseSet / WorkoutSessionExerciseSet**: thêm `target_duration_seconds`.
- **WorkoutSet**: thêm `duration_seconds`.

## Success Criteria *(mandatory)*

- **SC-001**: 100% bài `duration` hiển thị và ghi nhận theo thời gian, không hiển thị reps.
- **SC-002**: Hiệp `duration` lưu đúng giây đã nhập.
- **SC-003**: Rule Engine sinh đúng mục tiêu thời gian cho cardio/isometric.

## Assumptions

- Khoảng cách (distance) ngoài scope đợt này.
- Phân loại `measure_type` tự động theo category + danh sách isometric; Admin có thể sửa sau.
- Plank giữ nguyên mô hình sets (vd 3 hiệp × 60s).
