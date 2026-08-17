# Feature Specification: Quản trị Hệ thống (Admin)

**Feature Branch**: `006-admin-management`

**Created**: 2026-08-17

**Status**: Draft

**Input**: User description: "UC-18: Quản lý người dùng (Admin); UC-19: Quản lý nội dung bài tập (Admin) — Nhóm 9: Quản trị hệ thống"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tìm kiếm & quản lý tài khoản người dùng (Priority: P1)

Admin đăng nhập qua portal riêng muốn tìm kiếm người dùng theo email/tên/ID, xem chi tiết hồ sơ và lịch sử tập, thay đổi trạng thái tài khoản (khóa/mở khóa). Lệnh khóa có hiệu lực ngay khi người dùng kết nối lại online (kể cả khi đang offline tại thời điểm bị khóa) và token của tài khoản bị khóa bị vô hiệu hóa ngay. Mọi thao tác được ghi vào audit log.

**Why this priority**: Quản lý người dùng là tính năng cốt lõi của Admin portal — cần thiết cho vận hành hệ thống (xử lý vi phạm, hỗ trợ khách hàng).

**Independent Test**: Có thể kiểm thử bằng cách: Admin tìm kiếm User theo email → xem hồ sơ → khóa tài khoản → xác nhận trạng thái chuyển sang "banned" và audit log ghi nhận đúng.

**Acceptance Scenarios**:

1. **Given** Admin đã đăng nhập bằng tài khoản role Admin, **When** Admin tìm kiếm người dùng theo email, **Then** hệ thống trả danh sách kết quả kèm trạng thái tài khoản (active / banned / deleted) và trạng thái xác thực email (email_verified).
2. **Given** Admin đang xem danh sách kết quả, **When** Admin chọn 1 người dùng, **Then** hệ thống hiển thị chi tiết hồ sơ và lịch sử tập.
3. **Given** Admin đang xem chi tiết người dùng, **When** Admin nhấn "Khóa tài khoản" và nhập lý do, **Then** hệ thống cập nhật trạng thái sang "banned", vô hiệu hóa (revoke) ngay access token và refresh token của tài khoản, ghi audit log (ai, làm gì, lúc nào, lý do).
4. **Given** Admin đang xem người dùng bị khóa, **When** Admin nhấn "Mở khóa", **Then** hệ thống cập nhật trạng thái sang "active" và ghi audit log.
5. **Given** Tài khoản đã bị khóa (kể cả khi đang offline tại thời điểm bị khóa), **When** người dùng gửi request kế tiếp bất kỳ, **Then** middleware kiểm tra trạng thái ban và chặn request trong tối đa 3 giây kể từ request kế tiếp.

---

### User Story 2 - Quản lý nội dung bài tập (Priority: P1)

Admin muốn thêm mới, chỉnh sửa, hoặc ẩn bài tập trong thư viện. Bài tập bị ẩn sẽ biến mất khỏi Rule Engine (UC-05) và tìm kiếm của User nhưng vẫn hiển thị trong lịch sử tập cũ. Nếu bài tập bị ẩn đang nằm trong lộ trình active của người dùng, hệ thống thông báo cho người dùng, clone tạm thời bài tập vào draft queue để tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành) và gợi ý bài tập thay thế nếu có. Hệ thống tự động invalidate cache liên quan.

**Why this priority**: Nội dung bài tập là "hàng hóa" cốt lõi của ứng dụng. Admin cần khả năng duy trì và mở rộng thư viện bài tập liên tục.

**Independent Test**: Có thể kiểm thử bằng cách: Admin thêm mới 1 bài tập (tên, nhóm cơ, GIF, thumbnail) → xác nhận bài tập xuất hiện trong thư viện → Admin ẩn bài tập → xác nhận bài tập không xuất hiện trong tìm kiếm User.

**Acceptance Scenarios**:

1. **Given** Admin vào mục "Quản lý bài tập", **When** Admin nhấn "Thêm mới" và điền đầy đủ thông tin (tên, nhóm cơ, thiết bị cần, GIF, ảnh thumbnail 180×180, hướng dẫn từng bước), **Then** hệ thống lưu bài tập mới và bài tập xuất hiện trong thư viện.
2. **Given** Admin đang xem danh sách bài tập, **When** Admin chọn 1 bài tập và chỉnh sửa trường "Hướng dẫn", **Then** hệ thống lưu phiên bản mới và invalidate cache liên quan.
3. **Given** Admin đang xem chi tiết bài tập, **When** Admin nhấn "Ẩn", **Then** hệ thống chuyển trạng thái sang "inactive" — bài tập biến mất khỏi Rule Engine (UC-05) và tìm kiếm User, nhưng vẫn hiển thị trong lịch sử tập cũ. **And** nếu bài tập đang nằm trong lộ trình active của người dùng, hệ thống thông báo cho người dùng, clone tạm thời bài tập vào draft queue để tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành) và gợi ý bài tập thay thế (cùng nhóm cơ, cùng kiểu chuyển động, dụng cụ giống hoặc khác) nếu có.
4. **Given** Admin đã ẩn 1 bài tập, **When** User tạo Workout Plan mới, **Then** Rule Engine không chọn bài tập đã ẩn.
5. **Given** Admin thêm mới bài tập, **When** thiếu trường bắt buộc (ví dụ thiếu tên hoặc GIF), **Then** hệ thống hiển thị lỗi validation và không lưu.
6. **Given** Admin vào mục "Quản lý bài tập", **When** Admin import file dữ liệu bài tập (kể cả import lại cùng file), **Then** hệ thống upsert theo cặp (tên đã chuẩn hóa + equipment), không tạo bản ghi trùng và trả báo cáo inserted / updated / skipped.

---

### Edge Cases

- Điều gì xảy ra khi Admin khóa chính tài khoản Admin khác (có cho phép không)?
- Điều gì xảy ra khi Admin ẩn 1 bài tập đang có trong Workout Plan active của User? → Hệ thống thông báo cho người dùng, clone tạm thời bài tập vào draft queue để tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành) và gợi ý bài tập thay thế (cùng nhóm cơ, cùng kiểu chuyển động, dụng cụ giống hoặc khác) nếu có.
- Điều gì xảy ra khi upload GIF quá lớn hoặc định dạng không hỗ trợ?
- Điều gì xảy ra khi cần rollback thao tác Admin (ví dụ khóa nhầm tài khoản)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI cho phép Admin tìm kiếm người dùng theo email, tên, hoặc ID.
- **FR-002**: Hệ thống PHẢI hiển thị kết quả tìm kiếm kèm trạng thái tài khoản (active / banned / deleted) và trạng thái xác thực email (email_verified: true/false).
- **FR-003**: Hệ thống PHẢI cho phép Admin xem chi tiết hồ sơ và lịch sử tập của bất kỳ người dùng nào.
- **FR-004**: WHEN Admin khóa tài khoản người dùng kèm lý do bắt buộc, hệ thống PHẢI cập nhật trạng thái sang "banned"; lệnh khóa có hiệu lực ngay khi người dùng kết nối lại online, kể cả khi người dùng đang offline tại thời điểm bị khóa.
- **FR-005**: Hệ thống PHẢI cho phép Admin mở khóa tài khoản đã bị khóa.
- **FR-006**: Hệ thống PHẢI ghi mọi thao tác Admin vào audit log gồm: admin_id, action, target_user_id, reason (nếu có), timestamp.
- **FR-007**: WHEN Admin nhấn "Thêm mới" bài tập, hệ thống PHẢI yêu cầu nhập đầy đủ: tên, nhóm cơ, thiết bị cần, GIF, ảnh thumbnail (180×180), hướng dẫn từng bước.
- **FR-008**: Hệ thống PHẢI cho phép Admin chỉnh sửa bất kỳ trường nào của bài tập và lưu phiên bản mới.
- **FR-009**: WHEN Admin ẩn bài tập, hệ thống PHẢI chuyển trạng thái sang "inactive" — loại khỏi Rule Engine (UC-05) và tìm kiếm User, nhưng giữ nguyên trong lịch sử tập cũ. WHERE bài tập bị ẩn đang nằm trong lộ trình active của người dùng, hệ thống PHẢI thông báo cho người dùng, clone tạm thời bài tập vào draft queue để người dùng tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành), và gợi ý bài tập thay thế (cùng nhóm cơ, cùng kiểu chuyển động, dụng cụ giống hoặc khác) nếu có.
- **FR-010**: Hệ thống PHẢI tự động invalidate cache liên quan khi nội dung bài tập thay đổi.
- **FR-011**: Hệ thống PHẢI validate dữ liệu bài tập trước khi lưu (tên không trống, GIF URL hợp lệ, thumbnail đúng kích thước).
- **FR-012**: Hệ thống PHẢI kiểm tra trạng thái ban của tài khoản qua middleware ở MỌI request của người dùng và chặn request trong tối đa 3 giây kể từ request kế tiếp nếu tài khoản đang bị khóa.
- **FR-013**: WHEN tài khoản người dùng bị khóa, hệ thống PHẢI vô hiệu hóa (revoke) ngay access token và refresh token của tài khoản đó.
- **FR-014**: WHEN Admin import dữ liệu bài tập, hệ thống PHẢI xác định trùng lặp (duplicate) theo cặp (tên đã chuẩn hóa + equipment) và KHÔNG tạo bản ghi trùng.
- **FR-015**: WHEN Admin import lại cùng một file dữ liệu, hệ thống PHẢI thực hiện UPSERT: cập nhật bản ghi đã tồn tại, chèn bản ghi mới nếu chưa tồn tại.
- **FR-016**: Sau mỗi lần import, hệ thống PHẢI trả báo cáo kết quả gồm số lượng inserted / updated / skipped.

### Key Entities

- **Audit Log**: Bản ghi thao tác Admin (admin_id, action_type, target_type, target_id, reason, details_json, created_at).
- **Exercise (mở rộng)**: Thêm trường `status` (active/inactive) vào entity Exercise hiện tại.
- **Admin Action**: Enum các loại thao tác: ban_user, unban_user, create_exercise, update_exercise, deactivate_exercise.
- **Draft Queue**: Bản sao tạm thời của bài tập bị ẩn trong lộ trình active của người dùng, dùng để hoàn thành buổi tập hiện tại; bị xóa khi buổi tập hoàn thành.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Admin có thể tìm kiếm và xem chi tiết 1 người dùng trong vòng 10 giây.
- **SC-002**: 100% thao tác Admin (khóa/mở khóa/quản lý bài tập) được ghi đầy đủ trong audit log.
- **SC-003**: Bài tập bị ẩn biến mất khỏi tìm kiếm User và Rule Engine trong vòng 5 phút (sau khi cache invalidate).
- **SC-004**: Admin có thể thêm mới 1 bài tập hoàn chỉnh (điền form + upload GIF) trong vòng 2 phút.
- **SC-005**: Import lại cùng 1 file dữ liệu bài tập không tạo bản ghi trùng — tổng số bản ghi sau import bằng số bản ghi hợp lệ duy nhất (báo cáo inserted / updated / skipped).
- **SC-006**: Tài khoản bị khóa bị chặn ở request kế tiếp trong tối đa 3 giây; access token và refresh token bị vô hiệu hóa ngay khi Admin khóa.

## Assumptions

- Admin portal là giao diện web riêng biệt, không trộn lẫn với giao diện User.
- Chỉ tài khoản có role = "Admin" mới truy cập được Admin portal (kiểm tra qua JWT).
- Admin không thể khóa chính tài khoản Admin của mình (self-ban protection).
- Audit log chỉ cho phép đọc (read-only) — Admin không thể xóa hoặc sửa audit log.
- GIF và thumbnail được upload lên storage service (chi tiết kỹ thuật trong phase triển khai).
