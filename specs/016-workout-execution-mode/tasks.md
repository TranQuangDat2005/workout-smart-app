# Tasks: Màn tập trung tối giản cho buổi tập (016)

**Input**: Design documents from `/specs/016-workout-execution-mode/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/openapi.yaml ✅

**Tests**: BẮT BUỘC — constitution §9 yêu cầu coverage ≥ 80% phần code thay đổi + integration test happy/error path.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: chạy song song được (khác file, không phụ thuộc)
- **[Story]**: user story tương ứng (US1…US6)

## Phase 1: Setup

**Purpose**: Xác nhận nền tảng hiện có

- [X] T001 Kiểm tra `.specify/feature.json` trỏ `specs/016-workout-execution-mode` và backend/frontend đang chạy được (Task Scheduler WSBackend/WSFrontend)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: API delta dùng chung cho mọi user story

- [X] T002 Thêm `mediaUrl` (nullable) vào `backend/src/main/java/com/workoutsmart/tracking/dto/SessionExerciseResponse.java`; sửa `TrackingService.toResponse(...)` join `Exercise.gifUrl`/`image` theo map exerciseId (chống N+1)
- [X] T003 [P] Cập nhật typing `SessionExercise.mediaUrl` + thêm `trackingApi.deleteSet(sessionId, setId)` trong `web/src/services/trackingApi.ts`

**Checkpoint**: API đã có mediaUrl + deleteSet — các story có thể bắt đầu

---

## Phase 3: User Story 1 - Màn tập trung tối giản (P1) 🎯 MVP

**Goal**: Thay form hiện tại bằng layout 4 khối: tên bài + thời lượng → GIF → "Hiệp X - mục tiêu" → nút HOÀN THÀNH ghi hiệp theo target.

**Independent Test**: Bắt đầu buổi → thấy đúng 4 khối → 1 chạm HOÀN THÀNH → hiệp lưu + chuyển màn nghỉ.

- [X] T004 [US1] Viết lại `web/src/pages/training/WorkoutSession.tsx`: layout 4 khối (header tên bài + đồng hồ thời lượng từ `startTime` deadline-based; GIF/ảnh 180×180 từ `mediaUrl` fallback icon — đáp ứng US6; dòng "Hiệp X - target"; nút HOÀN THÀNH ghi reps=target, tạ=hiệp liền trước cùng bài, setType=target hiệp); giữ double-tap guard, báo phân tâm, kết thúc (confirm), khôi phục sau reload
- [X] T005 [US1] Component test `web/src/pages/training/WorkoutSession.test.tsx`: render 4 khối đúng thứ tự, double-tap guard chỉ gửi 1 request, ghi hiệp đúng target

**Checkpoint**: MVP — ghi hiệp 1 chạm hoạt động độc lập

---

## Phase 4: User Story 2 - Bài duration tự đếm ngược & tự chuyển (P1)

**Goal**: Bài tính giây hiển thị đồng hồ, hết giờ tự ghi + tự chuyển nghỉ.

**Independent Test**: Mở plank → Bắt đầu → hết giờ → hiệp tự ghi đúng giây → màn nghỉ tự hiện.

- [X] T006 [US2] Thêm `DurationTimer` trong `web/src/pages/training/WorkoutSession.tsx` (đếm ngược deadline-based, Bắt đầu/Tạm dừng, về 0 tự ghi `durationSeconds` đã đếm, khi pause hiện nút Lưu) + test fake timers

---

## Phase 5: User Story 3 - Màn nghỉ lấy thời gian từ DB (P1)

**Goal**: Màn nghỉ overlay đếm ngược từ `restTimeSeconds` của bài, beep 5s/0s, BỎ QUA; drop-set không nghỉ.

**Independent Test**: Bài có rest 90s → sau khi lưu hiệp thấy đếm từ 90 → BỎ QUA quay lại.

- [X] T007 [US3] Thêm `RestOverlay` trong `web/src/pages/training/WorkoutSession.tsx` (overlay trong container, countdown ≥ 2.5rem deadline-based, beep 5s/0s, BỎ QUA, drop-set skip) + test beep/đếm đúng rest từ DB

---

## Phase 6: User Story 4 - Hàng đợi chip & bài kế tiếp (P2)

**Goal**: Dải chip xong/đang/tới + "Tiếp theo: …", chuyển bài giữ trạng thái nhập riêng từng bài.

**Independent Test**: 3 bài → 3 chip → chạm chip 3 → màn chuyển bài 3.

- [X] T008 [US4] Thêm `ExerciseQueueChips` + dòng "Tiếp theo" trong `web/src/pages/training/WorkoutSession.tsx`; mỗi bài giữ riêng số hiệp/giá trị nhập; test chuyển bài

---

## Phase 7: User Story 5 - Sửa sai tối thiểu (P2)

**Goal**: Mini editor khi chạm dòng mục tiêu + hoàn tác hiệp trong 60s (endpoint DELETE backend).

**Independent Test**: Sửa reps → ghi đúng giá trị sửa; Hoàn tác trong 60s xóa hiệp, quá 60s ẩn nút.

- [X] T009 [US5] Backend: thêm `deleteSet(userId, sessionId, setId)` trong `backend/src/main/java/com/workoutsmart/tracking/service/TrackingService.java` (404 set/session không thuộc user, 409 session không active, xóa thành công) + `DELETE /workout-sessions/{id}/sets/{setId}` trong `backend/src/main/java/com/workoutsmart/tracking/controller/TrackingController.java`
- [X] T010 [US5] Backend test: unit test `TrackingServiceTest` (happy + 404 + 409 + set khác session) + integration test trong `TrackingControllerIntegrationTest`
- [X] T011 [US5] Frontend: `MiniEditor` (chạm dòng mục tiêu mở editor reps/tạ hoặc mm:ss) + nút "Hoàn tác" hiện 60s sau khi ghi gọi `trackingApi.deleteSet`; test hiển thị/ẩn theo thời gian

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validate toàn bộ theo quickstart + cleanup

- [X] T012 [P] Chạy `mvn test` (backend) — tất cả pass, gồm test mới
- [X] T013 [P] Chạy `npm --prefix web run build` + `npm --prefix web test` — pass
- [X] T014 Chạy kịch bản tay T1–T8 trong `quickstart.md`; restart WSBackend/WSFrontend; đánh dấu checklist

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (1)**: không phụ thuộc — bắt đầu ngay
- **Foundational (2)**: sau Setup — CHẶN mọi user story
- **User Stories (3–7)**: sau Foundational; thứ tự P1 → P2: US1 → US2 → US3 → US4 → US5 (US6 được đáp ứng bởi T002+T004)
- **Polish (8)**: sau tất cả story

### Parallel Opportunities

- T002 ∥ T003 (backend/frontend khác file)
- T005 ∥ T006 (cùng file WorkoutSession.test.tsx vs component mới — chạy tuần tự theo phase vì cùng file cha; thực tế T005 test file riêng, T006 code trong WorkoutSession.tsx → song song được)
- T009 (backend) ∥ T011 (frontend) — song song được

## Implementation Strategy

### MVP First (US1)

1. T001–T003 (foundation: mediaUrl + typing)
2. T004–T005 (màn tối giản + test) → **STOP & VALIDATE**: ghi hiệp 1 chạm
3. T006–T007 (duration + nghỉ) → validate auto-flow
4. T008–T011 (queue + sửa sai) → validate
5. T012–T014 (polish, build, test, quickstart)

## Notes

- [P] tasks khác file, không phụ thuộc
- Không migration mới; không sửa/xóa V13–V16
- Commit sau mỗi phase với Conventional Commits (feat: ...)
- Mọi task đánh `[X]` khi hoàn thành trong chính file này
