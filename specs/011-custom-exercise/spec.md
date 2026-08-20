# Feature Specification: Bài tập tự tạo cá nhân (Custom Exercise)

**Feature Branch**: `011-custom-exercise`

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Cho phép người tập tự tạo bài tập cá nhân ngoài kho hệ thống, riêng tư, dùng được trong tracking và Rule Engine; hỗ trợ upload media cho bài tập tự tạo."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tạo bài tập cá nhân (Priority: P1)

Người tập có một bài tập chưa có trong kho 1324 bài (biến thể máy, bài hồi phục, bài tại nhà...) muốn tự định nghĩa để tái sử dụng về sau. Hệ thống lưu bài tập vào kho cá nhân, hiển thị kèm đánh dấu "Của bạn", và chỉ chủ sở hữu mới thấy.

**Why this priority**: Đây là năng lực cốt lõi để kho bài tập bao phủ được thực tế tập luyện của từng người, tương tự custom food trong dinh dưỡng.

**Independent Test**: Đăng nhập → tạo bài tập "Kéo cáp 1 tay" với nhóm cơ lưng, dụng cụ máy → xác nhận bài tập xuất hiện trong thư viện tìm kiếm kèm badge "Của bạn".

**Acceptance Scenarios**:

1. **Given** User đã đăng nhập, **When** User tạo bài tập cá nhân với đầy đủ tên, nhóm cơ, dụng cụ (hướng dẫn/media tuỳ chọn), **Then** hệ thống lưu bài tập vào kho cá nhân và hiển thị thành công.
2. **Given** User nhập thiếu tên hoặc thiếu nhóm cơ hoặc thiếu dụng cụ, **When** User lưu, **Then** hệ thống từ chối kèm lỗi inline và không lưu.
3. **Given** User đã tạo bài tập cá nhân, **When** User tìm kiếm theo tên/nhóm cơ trong thư viện, **Then** kết quả hợp nhất bài tập hệ thống và bài tập cá nhân của User, bài cá nhân được đánh dấu "Của bạn".
4. **Given** Một User khác (không phải chủ sở hữu), **When** tìm kiếm hoặc mở chi tiết bài tập cá nhân của User A, **Then** hệ thống KHÔNG hiển thị bài tập đó.

---

### User Story 2 - Dùng bài tập cá nhân trong lộ trình (Priority: P1)

Người tập muốn bài tập cá nhân của mình được đưa vào lộ trình tự động (Rule Engine) như các bài hệ thống, miễn là khớp nhóm cơ/dụng cụ/cấu trúc theo mục tiêu.

**Why this priority**: Nếu bài tập cá nhân chỉ nằm trong thư viện mà không vào được lộ trình, giá trị thực tế của tính năng bị giảm mạnh.

**Independent Test**: Tạo bài tập cá nhân nhóm cơ ngực, dụng cụ tạ đơn → thiết lập mục tiêu "Tăng cơ" + thiết bị "Tạ đơn" → xác nhận bài tập cá nhân có thể xuất hiện trong lộ trình.

**Acceptance Scenarios**:

1. **Given** User có bài tập cá nhân khớp equipment và nhóm cơ của mục tiêu, **When** Rule Engine sinh lộ trình, **Then** bài tập cá nhân được đưa vào danh sách ứng viên như bài hệ thống.
2. **Given** Bài tập cá nhân đang nằm trong lộ trình active, **When** User xóa (soft-delete) bài tập đó, **Then** bài tập bị ẩn khỏi tìm kiếm và lộ trình mới, nhưng vẫn hiển thị trong lịch sử tập đã ghi.

---

### User Story 3 - Sửa & xóa bài tập cá nhân (Priority: P2)

Người tập muốn chỉnh sửa thông tin bài tập do chính mình tạo, hoặc xóa bài tập không còn dùng. Xóa phải an toàn (soft-delete) để không làm hỏng lịch sử tập cũ.

**Why this priority**: Khả năng duy trì kho cá nhân là cần thiết, nhưng không chặn việc sử dụng cơ bản (tạo + dùng) ở lần đầu.

**Independent Test**: Sửa tên bài tập cá nhân → xác nhận cập nhật; xóa bài tập → xác nhận ẩn khỏi tìm kiếm nhưng tên vẫn hiển thị trong lịch sử.

**Acceptance Scenarios**:

1. **Given** User là chủ sở hữu, **When** User sửa bài tập cá nhân và lưu, **Then** hệ thống cập nhật định nghĩa mới; các bản ghi lịch sử tập đã ghi giữ nguyên.
2. **Given** User là chủ sở hữu, **When** User xóa bài tập cá nhân, **Then** hệ thống soft-delete (`deleted_at`), ẩn khỏi tìm kiếm, không dùng cho bữa tập/lộ trình mới, nhưng giữ tên trong lịch sử tập cũ; sau 1 tuần bị xóa cứng.
3. **Given** User không phải chủ sở hữu, **When** User cố sửa/xóa bài tập cá nhân của người khác, **Then** hệ thống từ chối với HTTP 404/403.

---

### Edge Cases

- Điều gì xảy ra khi User tạo bài tập trùng tên + dụng cụ với bài tập hệ thống hoặc bài tập cá nhân của chính mình?
- Điều gì xảy ra khi upload file media quá lớn hoặc sai định dạng?
- Điều gì xảy ra khi media upload thất bại nhưng các trường văn bản hợp lệ?
- Điều gì xảy ra khi bài tập cá nhân bị xóa nhưng vẫn còn trong lộ trình active?
- Điều gì xảy ra khi User xóa bài tập cá nhân rồi lịch sử tập cũ cần hiển thị tên bài (retention)?
- Điều gì xảy ra khi kho bài tập hệ thống trống nhưng User đã có bài tập cá nhân?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: THE hệ thống SHALL cho phép User đã đăng nhập tạo bài tập cá nhân với các trường: tên (bắt buộc), nhóm cơ (bắt buộc), dụng cụ (bắt buộc), Category kho dữ liệu (bắt buộc — 012), hướng dẫn từng bước (tuỳ chọn), media GIF/ảnh (tuỳ chọn).
- **FR-002**: THE hệ thống SHALL validate dữ liệu bài tập cá nhân: tên không trống, nhóm cơ và dụng cụ hợp lệ; WHERE thiếu trường bắt buộc, THE hệ thống SHALL từ chối kèm lỗi inline.
- **FR-003**: THE hệ thống SHALL lưu bài tập cá nhân với `source = user_custom` và `created_by = userId`; bài tập cá nhân PHẢI riêng tư — chỉ chủ sở hữu nhìn thấy trong tìm kiếm và chi tiết.
- **FR-004**: THE hệ thống SHALL hợp nhất bài tập cá nhân của User vào kết quả tìm kiếm thư viện (cùng với bài tập hệ thống active) và đánh dấu nguồn gốc "Của bạn".
- **FR-005**: THE hệ thống SHALL cho phép bài tập cá nhân được Rule Engine chọn vào lộ trình tự động khi khớp `equipment`/`muscleGroup`/`category` với bảng luật v1.
- **FR-006**: THE hệ thống SHALL cho phép chủ sở hữu sửa bài tập cá nhân; các bản ghi tập cũ PHẢI giữ nguyên giá trị đã ghi.
- **FR-007**: WHEN chủ sở hữu xóa bài tập cá nhân, THE hệ thống SHALL soft-delete (`deleted_at`): ẩn khỏi tìm kiếm, KHÔNG dùng cho lộ trình/bài tập mới, nhưng tên vẫn hiển thị trong lịch sử tập cũ; sau 1 tuần THE hệ thống SHALL xóa cứng bản ghi.
- **FR-008**: THE hệ thống SHALL hỗ trợ upload media cho bài tập cá nhân (GIF/ảnh 180×180), validate định dạng/kích thước, lưu local/S3 và serve qua đường dẫn `/media/user/**`; media KHÔNG được gọi external API.
- **FR-009**: WHEN User cố sửa/xóa bài tập cá nhân không phải của mình, THE hệ thống SHALL từ chối (404/403) và không làm thay đổi dữ liệu.

### Key Entities

- **Exercise (mở rộng)**: thêm `source` (system/user_custom), `created_by` (nullable, FK users), `deleted_at` (nullable) để phân biệt bài tập hệ thống và bài tập cá nhân; giữ nguyên các trường hiện có (name, category, body_part, equipment, target, muscle_group, image, gif_url, instructions, status).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: User tạo được bài tập cá nhân trong vòng 60 giây và bài tập xuất hiện trong tìm kiếm ngay lập tức.
- **SC-002**: 100% bài tập cá nhân chỉ hiển thị cho đúng chủ sở hữu.
- **SC-003**: 100% bài tập cá nhân hợp lệ được đưa vào danh sách ứng viên của Rule Engine đúng theo equipment/muscleGroup.
- **SC-004**: 100% thao tác xóa bài tập cá nhân là soft-delete và không làm mất tên trong lịch sử tập cũ; bản ghi bị xóa cứng sau 1 tuần.

## Assumptions

- Bài tập cá nhân riêng tư (không chia sẻ, không kiểm duyệt, không đưa vào kho hệ thống).
- Media upload chỉ áp dụng cho bài tập cá nhân; kho hệ thống vẫn dùng `exercises-dataset/`.
- Lưu media local (thư mục cấu hình) cho MVP; có thể nâng cấp S3 sau.
- Rule Engine v1 không thay đổi bảng luật; chỉ mở rộng nguồn dữ liệu ứng viên để bao gồm bài tập cá nhân của User.
