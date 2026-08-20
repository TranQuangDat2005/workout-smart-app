# Feature Specification: Hiệp tập nâng cao (target từng hiệp & kiểu set)

**Feature Branch**: `014-advanced-sets`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Cho phép chỉnh rep theo từng set để có khả năng flex (pyramid), bổ sung kiểu set đặc biệt (warm-up, drop-set). Superset/giant set/rest-pause ngoài scope đợt này."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Đặt mục tiêu reps/tạ riêng từng hiệp trong template (Priority: P1)

Người tập muốn sửa một bài trong ngày lộ trình để mỗi hiệp có số reps mục tiêu (và tạ mục tiêu tùy chọn) khác nhau — ví dụ pyramid 12/10/8/6. Hiện tại chỉ có một số reps chung cho mọi hiệp.

**Why this priority**: Đây là "flex" thực sự khi lập kế hoạch; không có nó thì pyramid và các chuỗi tăng/giảm tải không thể biểu diễn được.

**Independent Test**: Mở Lộ trình → một ngày → sửa bài thành 4 hiệp với reps 12/10/8/6 → lưu → tải lại trang vẫn giữ đúng từng hiệp.

**Acceptance Scenarios**:

1. **Given** User có lộ trình active, **When** User sửa một bài và đặt target reps khác nhau cho từng hiệp (12/10/8/6), **Then** hệ thống lưu đúng thứ tự và số reps từng hiệp.
2. **Given** User đặt target reps từng hiệp kèm tạ mục tiêu (tùy chọn), **When** lưu ngày, **Then** hệ thống lưu cả tạ mục tiêu cho hiệp được chỉ định.
3. **Given** User không đặt target từng hiệp, **When** lưu ngày, **Then** hệ thống giữ cách cũ: một `target_reps` chung cho mọi hiệp.

---

### User Story 2 - Snapshot copy target từng hiệp và hiển thị target vs thực tế (Priority: P1)

Khi bắt đầu buổi tập, hệ thống copy target từng hiệp từ template vào buổi tập. Khi ghi hiệp, người tập thấy target reps/tạ của hiệp đó để so với số thực tế đã ghi.

**Why this priority**: Target từng hiệp chỉ có giá trị khi nó đi vào buổi tập thực tế; đây là điều kiện để "flex" không bị mất lúc tập.

**Independent Test**: Bắt đầu tập vào ngày có bài 12/10/8/6 → mở bài đó → thấy đúng 4 hiệp target 12/10/8/6 → ghi hiệp 1 = 10 reps → hệ thống vẫn hiển thị target 12 để so sánh.

**Acceptance Scenarios**:

1. **Given** Hôm nay có template bài với target từng hiệp, **When** User bắt đầu buổi tập, **Then** snapshot chứa đúng target reps/tạ theo thứ tự từng hiệp.
2. **Given** Buổi tập có target từng hiệp, **When** User ghi hiệp, **Then** dữ liệu thực tế (reps/tạ) được lưu độc lập và không ghi đè target.
3. **Given** Bài không có target từng hiệp, **When** bắt đầu buổi tập, **Then** hệ thống dùng `target_sets`/`target_reps` chung như hiện tại.

---

### User Story 3 - Kiểu hiệp warm-up không tính vào khối lượng (Priority: P2)

Người tập muốn đánh dấu một hiệp là khởi động (warm-up) để nó không bị tính vào tổng khối lượng nâng/thống kê.

**Why this priority**: Warm-up là nhu cầu phổ biến và rẻ để làm (một nhãn + loại khỏi tổng); giá trị là số liệu thống kê chính xác hơn.

**Independent Test**: Ghi hiệp 1 với nhãn warm-up và hiệp 2 normal → tổng khối lượng buổi chỉ tính hiệp normal.

**Acceptance Scenarios**:

1. **Given** User ghi một hiệp và đánh dấu `warm_up`, **When** hệ thống lưu hiệp, **Then** hiệp đó được lưu với `set_type = warm_up`.
2. **Given** Buổi tập có hiệp `warm_up`, **When** tính tổng khối lượng/thống kê, **Then** hiệp `warm_up` bị loại khỏi tổng khối lượng nâng.

---

### User Story 4 - Kiểu hiệp drop-set (nghỉ 0 giây) (Priority: P2)

Người tập muốn ghi một hiệp giảm tạ liền sau hiệp chính (drop-set) với thời gian nghỉ bằng 0.

**Why this priority**: Drop-set là kỹ thuật phổ biến; mô hình hóa đơn giản bằng `set_type = drop_set` + `rest = 0`, tái dùng đúng key UPSERT hiện tại.

**Independent Test**: Sau hiệp 3 normal, ghi hiệp 4 với nhãn drop-set → hệ thống lưu hiệp 4 có `set_type = drop_set` và `rest = 0`.

**Acceptance Scenarios**:

1. **Given** User ghi một hiệp và đánh dấu `drop_set`, **When** hệ thống lưu hiệp, **Then** hiệp đó có `set_type = drop_set` và thời gian nghỉ = 0.
2. **Given** Hiệp `drop_set` được ghi sau hiệp chính, **When** hệ thống lưu, **Then** hiệp này là một hiệp mới (set_number tăng) chứ không ghi đè hiệp chính.

---

### Edge Cases

- Bài trong template có target từng hiệp nhưng Admin ẩn bài sau khi snapshot → buổi đang tập vẫn giữ target đã copy.
- User gửi danh sách target từng hiệp có `set_number` trùng nhau → hệ thống từ chối (400).
- User gửi `set_type` không hợp lệ (khác normal/warm_up/drop_set) → hệ thống từ chối (400).
- Hiệp `drop_set` không có target trong template → hệ thống vẫn cho ghi (target không bắt buộc cho drop-set).
- Target từng hiệp rỗng nhưng `target_sets` > 0 → dùng target chung.
- Khi ghi hiệp `drop_set`, client không bắt đầu đồng hồ nghỉ (rest = 0).
- Khi chuyển sang bài tập khác, ô "Tạ (kg)" phải reset (không giữ tạ của bài trước).
- Khi kết thúc buổi tập, client phải xác nhận trước khi đánh dấu hoàn thành.
- Khi đổi ngày trong template, chi tiết bài tập đang mở phải đóng.
- Thêm nhiều bài liên tiếp không được tạo ID trùng (tránh lỗi kéo-thả/xóa nhầm).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: THE hệ thống SHALL cho phép chủ lộ trình đặt target reps (và target tạ tùy chọn) khác nhau cho từng hiệp của một bài trong ngày template.
- **FR-002**: WHEN User lưu một ngày template, THE hệ thống SHALL thay thế toàn bộ target từng hiệp của từng bài theo thứ tự đã gửi; buổi tập đã start KHÔNG bị ghi đè.
- **FR-003**: WHEN User bắt đầu buổi tập, THE hệ thống SHALL copy target từng hiệp từ template vào snapshot thuộc buổi đó, giữ nguyên thứ tự; WHERE bài không có target từng hiệp, THE hệ thống SHALL dùng `target_sets`/`target_reps` chung.
- **FR-004**: THE hệ thống SHALL cho phép ghi reps/tạ thực tế độc lập cho từng hiệp và hiển thị target reps/tạ để so sánh; dữ liệu thực tế KHÔNG ghi đè target.
- **FR-005**: THE hệ thống SHALL cho phép đánh dấu hiệp là `normal`, `warm_up` hoặc `drop_set`; hiệp `warm_up` KHÔNG được tính vào tổng khối lượng nâng/thống kê.
- **FR-006**: WHEN User ghi hiệp `drop_set`, THE hệ thống SHALL đặt thời gian nghỉ = 0 và cho phép ghi thêm hiệp mới (set_number tăng dần).
- **FR-007**: THE hệ thống SHALL validate `set_type` chỉ nhận `normal`/`warm_up`/`drop_set`; giá trị khác SHALL trả 400 validation.
- **FR-008**: Ghi hiệp SHALL vẫn UPSERT theo `(session_id, session_exercise_id, set_number)` theo Last-Write-Wins; KHÔNG thay đổi invariant sync đã chốt.
- **FR-009**: WHEN ghi hiệp `drop_set`, client SHALL không bắt đầu đồng hồ nghỉ (coi rest = 0).
- **FR-010**: WHEN User chuyển sang bài tập khác trong buổi tập, client SHALL reset ô tạ về rỗng.
- **FR-011**: WHEN User nhấn "Kết thúc buổi tập", client SHALL yêu cầu xác nhận trước khi đánh dấu hoàn thành.
- **FR-012**: WHEN hiển thị nút bấm trong form, client SHALL mặc định `type="button"` để tránh submit form ngoài ý muốn.

### Key Entities

- **WorkoutPlanExerciseSet**: target một hiệp trong template (số hiệp, reps mục tiêu, tạ mục tiêu tùy chọn, kiểu set).
- **WorkoutSessionExerciseSet**: bản snapshot target một hiệp của buổi tập.
- **WorkoutSet**: hiệp thực tế, bổ sung `set_type` (normal/warm_up/drop_set).
- **SetType**: enum `normal`, `warm_up`, `drop_set`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% target từng hiệp được copy đúng thứ tự khi bắt đầu buổi tập.
- **SC-002**: Hiệp `warm_up` không làm tăng tổng khối lượng nâng của buổi tập.
- **SC-003**: Hiệp `drop_set` được lưu với thời gian nghỉ = 0.
- **SC-004**: User sửa target từng hiệp và thấy kết quả giữ sau khi tải lại trang.

## Assumptions

- Web triển khai trước; Mobile làm sau.
- Không tự động tính tạ theo %1RM; target tạ là nhập tay (optional).
- Superset / giant set / circuit / rest-pause / failure tracking ngoài scope đợt này.
- Báo cáo "% đạt target" ngoài scope — chỉ hiển thị target vs thực tế trong buổi tập.
- `target_weight` có thể bỏ trống (null) với mọi hiệp.
