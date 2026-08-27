# Feature Specification: Thống kê & Báo cáo Tiến độ

**Feature Branch**: `005-stats-reports`

**Created**: 2026-08-17

**Status**: Draft

**Input**: User description: "UC-17: Xem thống kê & báo cáo tiến độ — Nhóm 8: Thống kê & Báo cáo tiến độ"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem dashboard thống kê tổng quan (Priority: P1)

Người dùng đã có dữ liệu tập luyện và/hoặc chỉ số cơ thể muốn xem tổng quan tiến độ qua các biểu đồ trực quan: cân nặng theo thời gian, volume tổng kg nâng/tuần, streak (chuỗi tuần liên tiếp đạt ≥ 3 buổi tập), tỷ lệ hoàn thành Workout Plan (tính cả plan active và archived), và calo tiêu thụ vs. calo nạp.

**Why this priority**: Dashboard thống kê là tính năng giúp User nhìn thấy "kết quả" của nỗ lực — yếu tố duy trì động lực hàng đầu. Dữ liệu tổng hợp từ UC-07, UC-12, UC-13.

**Independent Test**: Có thể kiểm thử bằng cách: User hoàn thành ít nhất 3 buổi tập + 1 lần cập nhật chỉ số → vào Thống kê → xác nhận tất cả biểu đồ hiển thị dữ liệu chính xác.

**Acceptance Scenarios**:

1. **Given** User đã có ít nhất 1 buổi tập hoàn thành, **When** User vào mục "Thống kê", **Then** hệ thống hiển thị dashboard gồm: biểu đồ volume, streak, tỷ lệ hoàn thành Plan.
2. **Given** User đã có dữ liệu chỉ số cơ thể (UC-13), **When** User xem biểu đồ cân nặng, **Then** hệ thống hiển thị biểu đồ đường thể hiện cân nặng theo thời gian.
3. **Given** User đã ghi nhận bữa ăn (UC-12), **When** User xem mục calo, **Then** hệ thống hiển thị biểu đồ so sánh calo tiêu thụ vs. calo nạp trung bình 7 ngày gần nhất.
4. **Given** User chưa có dữ liệu nào, **When** User vào mục "Thống kê", **Then** hệ thống hiển thị trạng thái trống với thông báo hướng dẫn bắt đầu tập luyện/ghi nhận dữ liệu.

---

### User Story 2 - Lọc thống kê theo khoảng thời gian (Priority: P2)

Người dùng muốn lọc dữ liệu thống kê theo các khoảng thời gian khác nhau (7 ngày / 30 ngày / 90 ngày / tùy chọn) để so sánh tiến độ giữa các giai đoạn.

**Why this priority**: Khả năng lọc thời gian giúp User thấy xu hướng dài hạn và so sánh giai đoạn, nhưng không phải tính năng bắt buộc cho lần đầu sử dụng.

**Independent Test**: Có thể kiểm thử bằng cách: User có dữ liệu 30 ngày → chọn lọc "7 ngày" → xác nhận biểu đồ chỉ hiển thị dữ liệu 7 ngày gần nhất → chọn "30 ngày" → xác nhận hiển thị đầy đủ.

**Acceptance Scenarios**:

1. **Given** User đang xem dashboard thống kê, **When** User chọn bộ lọc "7 ngày", **Then** tất cả biểu đồ cập nhật để chỉ hiển thị dữ liệu trong 7 ngày gần nhất.
2. **Given** User đang xem dashboard thống kê, **When** User chọn bộ lọc "Tùy chọn" và chọn khoảng ngày cụ thể, **Then** biểu đồ hiển thị dữ liệu trong khoảng thời gian đã chọn.

---

### Edge Cases

- Điều gì xảy ra khi User có dữ liệu buổi tập nhưng chưa có dữ liệu chỉ số cơ thể (chỉ hiển thị biểu đồ có dữ liệu)?
- Điều gì xảy ra khi User chọn khoảng thời gian không có dữ liệu nào?
- Điều gì xảy ra khi dữ liệu calo nạp không đầy đủ (không ghi nhận đủ 7 ngày)?
- Điều gì xảy ra khi biểu đồ calo cần dữ liệu cũ hơn 2 tuần (chỉ còn tổng calo ngày từ `meal_daily_summaries`, không có chi tiết macro)?

## Requirements *(mandatory)*

### Functional Requirements

### Clarifications

- `completed` là trạng thái session, không đồng nghĩa với đạt toàn bộ target.
- Hệ thống phải hiển thị thêm target attainment theo từng set: set reps đạt khi actual reps >= target reps; set duration đạt khi actual duration >= target duration.
- Target attainment dùng actual data của các set đã ghi; cho phép actual bằng 0 hoặc vượt target nếu payload đúng kiểu bài.

- **FR-001**: Hệ thống PHẢI hiển thị biểu đồ cân nặng theo thời gian (line chart) sử dụng dữ liệu từ UC-13.
- **FR-002**: Hệ thống PHẢI hiển thị biểu đồ volume tổng kg nâng theo tuần (bar chart) sử dụng dữ liệu từ `workout_sets`.
- **FR-003**: Hệ thống PHẢI hiển thị streak — chuỗi TUẦN liên tiếp đạt ≥ 3 buổi tập (định nghĩa duy nhất toàn hệ thống), gồm streak hiện tại và streak dài nhất. Streak KHÔNG tính theo số ngày tập liên tiếp.
- **FR-004**: Hệ thống PHẢI tính và hiển thị tỷ lệ hoàn thành Workout Plan (% buổi tập đã hoàn thành / tổng số buổi trong kế hoạch). Tỷ lệ này được tổng hợp từ cả Plan hiện tại (active) và các Plan cũ (archived).
- **FR-005**: Hệ thống PHẢI hiển thị biểu đồ so sánh calo tiêu thụ vs. calo nạp trung bình 7 ngày gần nhất (dữ liệu từ UC-12 và ước tính calo tiêu thụ từ buổi tập). Đối với dữ liệu cũ hơn 2 tuần, calo nạp PHẢI lấy từ bảng tổng kết ngày `meal_daily_summaries` (chỉ có tổng calo, không có chi tiết macro).
- **FR-006**: Hệ thống PHẢI hỗ trợ lọc dữ liệu theo khoảng thời gian: 7 ngày, 30 ngày, 90 ngày, và tùy chọn (date range picker).
- **FR-007**: Hệ thống PHẢI hiển thị trạng thái trống với hướng dẫn khi không có dữ liệu cho bất kỳ biểu đồ nào.

### Key Entities

- **Stats Dashboard**: Tổng hợp dữ liệu thống kê từ nhiều nguồn (workout_sessions, body_metrics, meal_logs, meal_daily_summaries) cho một User.
- **Time Period Filter**: Bộ lọc thời gian (7d, 30d, 90d, custom range) áp dụng cho tất cả biểu đồ.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Dashboard thống kê tải đầy đủ tất cả biểu đồ trong vòng 3 giây với dữ liệu 90 ngày.
- **SC-002**: 100% biểu đồ hiển thị dữ liệu chính xác khớp với dữ liệu gốc trong database.
- **SC-003**: User có thể chuyển đổi bộ lọc thời gian và thấy biểu đồ cập nhật trong vòng 1 giây.
- **SC-004**: 80% User sử dụng tính năng thống kê ít nhất 1 lần/tuần (đo lường engagement).

## Assumptions

- Dữ liệu thống kê được tính toán theo batch (pre-computed) và cache để đảm bảo hiệu năng, không tính realtime trên mỗi request.
- Biểu đồ sử dụng thư viện chart phía client (Chart.js hoặc tương đương) — chi tiết kỹ thuật trong phase triển khai.
- Calo tiêu thụ trong buổi tập được ước tính dựa trên loại bài tập + thời lượng (công thức MET hoặc đơn giản hóa).
- Khi thiếu dữ liệu một phần (ví dụ chỉ có workout nhưng không có nutrition), biểu đồ tương ứng sẽ bị ẩn thay vì hiển thị trống.
