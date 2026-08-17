# Feature Specification: Theo dõi buổi tập & Kiểm soát tập trung

**Feature Branch**: `009-workout-tracking`

**Created**: 2026-08-17

**Status**: Draft

**Input**: User description: "UC-07: Bắt đầu buổi tập & ghi nhận hiệp; UC-08: Rest Timer & cảnh báo; UC-09: Phát hiện phân tâm (Focus Detection) — Nhóm 3: Workout Tracking"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ghi nhận hiệp tập (Priority: P1)

Người dùng đang trong buổi tập muốn ghi lại từng hiệp (số lần lặp, khối lượng tạ) để theo dõi tiến độ. Khi hoàn thành một hiệp, hệ thống tự động kích hoạt đồng hồ đếm ngược thời gian nghỉ.

**Why this priority**: Ghi nhận hiệp tập là dữ liệu nền tảng cho thống kê (UC-17), lịch sử (UC-11) và động lực tập luyện. Đây là hoạt động cốt lõi trong mỗi buổi tập.

**Independent Test**: Có thể kiểm thử bằng cách bắt đầu buổi tập → nhấn "Hoàn thành hiệp" → xác nhận dữ liệu hiệp được lưu và đồng hồ nghỉ tự động bắt đầu.

**Acceptance Scenarios**:

1. **Given** Người dùng đang trong buổi tập, **When** người dùng nhấn "Hoàn thành hiệp", **Then** hệ thống lưu dữ liệu hiệp (số lần, khối lượng) và tự động bắt đầu đồng hồ đếm ngược thời gian nghỉ.
2. **Given** Người dùng có buổi tập trước đó còn status active, **When** người dùng cố bắt đầu buổi tập mới, **Then** hệ thống hiển thị cảnh báo "Bạn có một buổi tập chưa hoàn thành. Tiếp tục hay kết thúc?" (không áp dụng cho session đã expired).
3. **Given** Kết nối mạng bị gián đoạn, **When** người dùng lưu hiệp tập, **Then** hệ thống lưu vào hàng đợi offline và tự động sync khi thiết bị khôi phục kết nối mạng (reconnect event — KHÔNG periodic retry, xem UC-21).

---

### User Story 2 - Đồng hồ nghỉ (Rest Timer) (Priority: P1)

Hệ thống tự động đếm ngược thời gian nghỉ giữa các hiệp và phát cảnh báo ở 5 giây cuối để người dùng chuẩn bị cho hiệp tiếp theo, giúp kiểm soát nhịp độ và tránh nghỉ quá lâu.

**Why this priority**: Rest Timer là tính năng chính giải quyết bài toán "mất tập trung, nghỉ quá lâu", trực tiếp phục vụ success metric về độ tập trung.

**Independent Test**: Có thể kiểm thử bằng cách hoàn thành 1 hiệp → xác nhận đồng hồ nghỉ chạy → ở 5 giây cuối có cảnh báo âm thanh/rung.

**Acceptance Scenarios**:

1. **Given** Người dùng vừa hoàn thành một hiệp, **When** đồng hồ nghỉ bắt đầu, **Then** hệ thống hiển thị đếm ngược thời gian nghỉ còn lại.
2. **Given** Đồng hồ nghỉ đang chạy, **When** còn 5 giây cuối, **Then** hệ thống phát cảnh báo âm thanh/rung trên thiết bị di động.
3. **Given** Đồng hồ nghỉ kết thúc, **When** thời gian về 0, **Then** hệ thống phát tín hiệu kết thúc nghỉ để người dùng tiếp tục tập.

---

### User Story 3 - Phát hiện phân tâm (Focus Detection) (Priority: P2)

Hệ thống phát hiện khi người dùng rời khỏi màn hình tập luyện trong lúc đang tập (không tính lúc nghỉ) và ghi nhận số lần phân tâm, đồng thời nhắc nhở trên Mobile để kéo người dùng quay lại.

**Why this priority**: Focus Detection cung cấp dữ liệu cho success metric "60% người dùng tập trung hơn" và giúp duy trì động lực, nhưng là tính năng bổ trợ sau khi tracking cơ bản hoạt động.

**Independent Test**: Có thể kiểm thử bằng cách thu nhỏ ứng dụng Flutter khi đang tập quá 15 giây → xác nhận hệ thống cộng 1 vào số lần phân tâm và gửi thông báo nhắc nhở.

**Acceptance Scenarios**:

1. **Given** Người dùng đang tập (không trong thời gian nghỉ), **When** người dùng rời màn hình liên tục quá 15 giây, **Then** hệ thống cộng 1 vào số lần phân tâm.
2. **Given** Người dùng trên Mobile rời màn hình quá 15 giây khi đang tập, **When** hệ thống phát hiện phân tâm, **Then** hệ thống gửi thông báo đẩy nhắc nhở "Đừng phân tâm, quay lại tập nào!".
3. **Given** Người dùng trên Web chuyển tab sang tab khác quá 15 giây khi đang tập, **When** hệ thống phát hiện, **Then** hệ thống cộng 1 vào số lần phân tâm.
4. **Given** Đồng hồ nghỉ đang chạy, **When** người dùng rời màn hình, **Then** hệ thống KHÔNG tính là phân tâm.

---

### User Story 4 - Kết thúc buổi tập (Priority: P1)

Người dùng muốn kết thúc buổi tập để hoàn tất phiên, lưu trạng thái hoàn thành và xem tổng kết buổi tập.

**Why this priority**: Kết thúc buổi tập là bước đóng phiên, cần thiết để dữ liệu session chính xác phục vụ thống kê và lịch sử.

**Independent Test**: Có thể kiểm thử bằng cách hoàn thành các hiệp → nhấn "Kết thúc buổi tập" → xác nhận session được đánh dấu hoàn thành và hiển thị tổng kết.

**Acceptance Scenarios**:

1. **Given** Người dùng đang trong buổi tập, **When** người dùng nhấn "Kết thúc buổi tập", **Then** hệ thống đánh dấu session hoàn thành và hiển thị tổng kết (thời lượng, số hiệp, tổng khối lượng).
2. **Given** Admin ban User đang giữa buổi tập (kể cả khi User đang offline lúc bị ban), **When** User gửi request kế tiếp hoặc kết nối lại online, **Then** hệ thống chặn User ở request kế tiếp (độ trễ tối đa 3 giây), giữ dữ liệu đã ghi và đánh dấu session "interrupted".
3. **Given** Buổi tập còn status active, **When** đã sang ngày mới (giờ địa phương) so với ngày bắt đầu, **Then** hệ thống tự động đánh dấu session là "expired".

---

### Edge Cases

- Điều gì xảy ra khi người dùng đăng nhập trên 2 thiết bị và ghi hiệp tập đồng thời (sync conflict)? (Xử lý duy nhất theo Last-Write-Wins, không cảnh báo conflict cho User — xem FR-009.)
- Điều gì xảy ra khi thiết bị hết pin giữa buổi tập?
- Điều gì xảy ra khi người dùng để buổi tập mở qua đêm không kết thúc (bỏ quên buổi tập)? (Session tự động chuyển sang expired khi sang ngày mới theo giờ địa phương — xem FR-010.)
- Điều gì xảy ra khi người dùng nhấn nhanh nhiều lần nút "Hoàn thành hiệp"? (Client chỉ gửi đúng 1 request nhờ kiểm tra trạng thái "đang gửi" — xem FR-011.)
- Điều gì xảy ra khi thời gian nghỉ được cấu hình khác nhau cho từng bài tập?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: WHEN người dùng nhấn "Hoàn thành hiệp", hệ thống PHẢI lưu dữ liệu hiệp (số lần, khối lượng) và tự động bắt đầu đồng hồ đếm ngược thời gian nghỉ.
- **FR-002**: WHILE đồng hồ nghỉ đang chạy, hệ thống PHẢI phát cảnh báo âm thanh/rung ở 5 giây cuối trên thiết bị di động.
- **FR-003**: WHERE người dùng rời màn hình bài tập liên tục quá 15 giây trong lúc đang tập (không tính lúc nghỉ), hệ thống PHẢI cộng 1 vào số lần phân tâm.
- **FR-004**: Trên Web, hệ thống PHẢI phát hiện chuyển tab (document không còn hiển thị) để tính phân tâm.
- **FR-005**: Trên Mobile, hệ thống PHẢI phát hiện app chuyển nền/không hoạt động để tính phân tâm và gửi thông báo đẩy nhắc nhở.
- **FR-006**: WHEN người dùng cố bắt đầu buổi tập mới trong khi buổi trước còn status active, hệ thống PHẢI hiển thị cảnh báo tiếp tục hay kết thúc (không áp dụng cho session đã expired).
- **FR-007**: WHEN Admin ban User đang giữa buổi tập, hệ thống PHẢI chặn User ở request kế tiếp (độ trễ tối đa 3 giây), giữ dữ liệu đã ghi và đánh dấu session "interrupted". Lệnh ban PHẢI có hiệu lực ngay khi User kết nối lại online (kể cả khi User đang offline lúc bị ban).
- **FR-008**: WHEN mất mạng trong lúc lưu hiệp tập, hệ thống PHẢI lưu vào hàng đợi offline. WHEN thiết bị khôi phục kết nối mạng, hệ thống PHẢI tự động trigger sync (push queue lên server) — CHỈ sync khi reconnect event, KHÔNG dùng periodic retry (xem UC-21 trong General Spec).
- **FR-009**: WHEN xảy ra xung đột sync (đăng nhập 2 thiết bị), hệ thống PHẢI xử lý DUY NHẤT theo Last-Write-Wins (bản ghi đến sau thắng) dựa trên timestamp; với dữ liệu hiệp tập dùng UPSERT theo (session_id, set_number). Hệ thống KHÔNG hiển thị cảnh báo conflict cho User.
- **FR-010**: WHEN bắt đầu ngày mới theo giờ địa phương, WHERE buổi tập vẫn còn status active, hệ thống PHẢI tự động đánh dấu session đó là expired (status enum: active/completed/interrupted/expired).
- **FR-011**: WHEN người dùng nhấn nhanh nhiều lần nút "Hoàn thành hiệp" (hoặc nút lưu tương tự), client PHẢI kiểm tra trạng thái "đang gửi" và chỉ gửi đúng 1 request, KHÔNG tạo bản ghi hiệp trùng lặp.

### Key Entities

- **Workout Session**: Một buổi tập của người dùng với thời gian bắt đầu/kết thúc, status (active/completed/interrupted/expired), số lần phân tâm.
- **Workout Set**: Một hiệp tập với số lần hoàn thành, khối lượng đã dùng, thời gian nghỉ.
- **Focus Interruption**: Bản ghi số lần phân tâm trong buổi tập.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% hiệp tập được lưu chính xác khi người dùng nhấn "Hoàn thành hiệp".
- **SC-002**: Đồng hồ nghỉ bắt đầu trong vòng 1 giây sau khi hoàn thành hiệp.
- **SC-003**: Cảnh báo 5 giây cuối phát chính xác ở mọi thiết bị di động.
- **SC-004**: Dữ liệu offline được đồng bộ lại chính xác sau khi có mạng trở lại.

## Assumptions

- Thời gian nghỉ mặc định được cấu hình chung cho mọi bài tập (có thể tùy chỉnh sau).
- Phát hiện phân tâm trên Web dựa trên trạng thái hiển thị tab, trên Mobile dựa trên trạng thái vòng đời app.
- Offline queue lưu dữ liệu hiệp tập và đồng bộ khi thiết bị khôi phục kết nối mạng (reconnect event — xem UC-21 trong General Spec).
- Người dùng có thể bắt đầu buổi tập dựa trên lộ trình đã tạo hoặc chọn bài tập tự do.
- Ranh giới "ngày mới" (dùng cho auto-expire session) tính theo giờ địa phương của thiết bị người dùng.
