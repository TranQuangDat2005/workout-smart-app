# Feature Specification: Quản lý Hồ sơ & Lịch sử tập

**Feature Branch**: `001-profile-history`

**Created**: 2026-08-17

**Status**: Draft

**Input**: User description: "UC-10: Xem & chỉnh sửa hồ sơ cá nhân; UC-11: Xem lịch sử buổi tập — Nhóm 4: Quản lý hồ sơ & lịch sử tập"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Xem & chỉnh sửa hồ sơ cá nhân (Priority: P1)

Người dùng đã đăng nhập muốn xem và cập nhật thông tin cá nhân (tên, ảnh đại diện, tuổi, cân nặng, chiều cao, mục tiêu hiện tại) để hệ thống cá nhân hóa lộ trình tập phù hợp. Khi thay đổi mục tiêu (ví dụ từ giảm cân sang tăng cơ), hệ thống cảnh báo rằng việc lưu trữ (archive) Workout Plan cũ sẽ làm mất tiến trình tập của ngày hôm nay, và đưa ra 2 lựa chọn: (1) tạo plan mới ngay, hoặc (2) plan mới bắt đầu từ ngày mai.

**Why this priority**: Hồ sơ cá nhân là nền tảng cho mọi tính năng cá nhân hóa (lộ trình, TDEE, thống kê). Thay đổi mục tiêu phải trigger tái tạo plan — liên kết trực tiếp với UC-05.

**Independent Test**: Có thể kiểm thử độc lập bằng cách đăng nhập, vào hồ sơ, sửa 1 trường → xác nhận thay đổi lưu thành công và phản ánh trên giao diện.

**Acceptance Scenarios**:

1. **Given** User đã đăng nhập, **When** User vào mục "Hồ sơ cá nhân", **Then** hệ thống hiển thị đầy đủ thông tin: tên, ảnh đại diện, tuổi, cân nặng, chiều cao, mục tiêu hiện tại (trong đó cân nặng đồng bộ từ dữ liệu lịch sử đo).
2. **Given** User đang ở trang hồ sơ, **When** User chỉnh sửa tuổi và chiều cao, **Then** hệ thống validate dữ liệu, cập nhật thành công. (Ghi chú: Cân nặng KHÔNG được phép sửa ở đây).
3. **Given** User đang ở trang hồ sơ, **When** User thay đổi mục tiêu từ "Giảm cân" sang "Tăng cơ" và nhấn "Lưu", **Then** hệ thống hiển thị cảnh báo "Việc tạo lại plan mới sẽ làm mất tiến trình tập của ngày hôm nay" kèm 2 lựa chọn: (1) "Tạo plan mới ngay" — plan cũ chuyển sang archived và plan mới active ngay lập tức; (2) "Bắt đầu từ ngày mai" — plan cũ tiếp tục có hiệu lực đến hết hôm nay, plan mới có hiệu lực từ ngày mai. Lịch sử tập cũ vẫn giữ nguyên liên kết với plan cũ.
4. **Given** User vào mục Cài đặt tài khoản, **When** User yêu cầu Xóa tài khoản, **Then** hệ thống yêu cầu xác nhận mật khẩu, sau đó chuyển tài khoản sang trạng thái soft-delete (giữ 30 ngày) và đăng xuất. Soft-delete chỉ thay đổi trạng thái hiển thị: mọi liên kết dữ liệu (lịch sử tập, bạn bè, feed, dinh dưỡng) được giữ nguyên, chỉ bị ẩn khỏi các bảng khác; trong 30 ngày User có thể hủy yêu cầu xóa.
5. **Given** Tài khoản đang ở trạng thái soft-delete, **When** User yêu cầu khôi phục (hủy yêu cầu xóa) trong vòng 30 ngày, **Then** hệ thống khôi phục tài khoản về trạng thái hoạt động với toàn bộ dữ liệu nguyên vẹn (chỉ đổi lại trạng thái, không tạo tài khoản mới).

---

### User Story 2 - Xem lịch sử buổi tập (Priority: P1)

Người dùng đã hoàn thành ít nhất 1 buổi tập muốn xem lại toàn bộ nhật ký tập luyện, bao gồm danh sách các buổi tập theo thứ tự thời gian ngược và chi tiết từng buổi (bài tập, hiệp, reps, weight).

**Why this priority**: Lịch sử tập là động lực quan trọng giúp người dùng theo dõi sự tiến bộ và duy trì thói quen. Dữ liệu lịch sử là nguồn đầu vào cho UC-17 (Thống kê & Báo cáo).

**Independent Test**: Có thể kiểm thử bằng cách hoàn thành 1 buổi tập → vào Lịch sử → xác nhận buổi tập vừa hoàn thành xuất hiện đúng dữ liệu.

**Acceptance Scenarios**:

1. **Given** User đã hoàn thành ít nhất 1 buổi tập, **When** User vào mục "Lịch sử tập", **Then** hệ thống hiển thị danh sách buổi tập theo thứ tự thời gian ngược (ngày, thời lượng, số hiệp, tổng khối lượng nâng).
2. **Given** User đang xem danh sách lịch sử, **When** User chọn 1 buổi tập cụ thể, **Then** hệ thống hiển thị chi tiết từng bài tập, từng hiệp (reps, weight).
3. **Given** User chưa hoàn thành buổi tập nào, **When** User vào mục "Lịch sử tập", **Then** hệ thống hiển thị trạng thái trống với thông báo hướng dẫn bắt đầu buổi tập đầu tiên.

---

### Edge Cases

- Điều gì xảy ra khi User upload ảnh đại diện quá lớn (>5 MB)?
- Điều gì xảy ra khi User nhập chiều cao = 0 cm hoặc tuổi > 120?
- Điều gì xảy ra khi lịch sử tập có hàng trăm buổi — cần phân trang hay cuộn vô hạn?
- Điều gì xảy ra khi User xóa tài khoản — lịch sử tập có bị xoá vĩnh viễn không? (Lịch sử tập chỉ bị ẩn trong 30 ngày soft-delete và chỉ bị xóa vĩnh viễn sau 30 ngày khi hard-delete.)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI hiển thị đầy đủ thông tin hồ sơ cá nhân gồm: tên, ảnh đại diện, tuổi, cân nặng (đồng bộ từ lịch sử đo), chiều cao (cm), mục tiêu hiện tại khi User truy cập trang hồ sơ. Hồ sơ mặc định ở chế độ Private (chỉ bạn bè mới xem được).
- **FR-002**: Hệ thống PHẢI cho phép User chỉnh sửa một hoặc nhiều trường hồ sơ (trừ cân nặng) đồng thời và lưu trong một lần thao tác.
- **FR-003**: Hệ thống PHẢI validate dữ liệu đầu vào: chiều cao > 0, tuổi trong khoảng 10–120 tuổi. Nếu không hợp lệ, hiển thị lỗi inline tại trường tương ứng và không lưu.
- **FR-004**: WHEN User thay đổi mục tiêu (goal_type), hệ thống PHẢI cảnh báo rằng việc lưu trữ (archive) Workout Plan cũ sẽ làm mất tiến trình tập của ngày hôm nay và PHẢI đưa ra 2 lựa chọn cho User: (1) tạo plan mới ngay (plan cũ → archived, plan mới active), hoặc (2) plan mới bắt đầu từ ngày mai (plan cũ tiếp tục có hiệu lực đến hết ngày hôm nay) (extend UC-05). Lịch sử tập cũ vẫn giữ nguyên liên kết với plan cũ.
- **FR-005**: WHEN User yêu cầu xóa tài khoản, hệ thống PHẢI chuyển trạng thái tài khoản sang soft-delete (giữ 30 ngày) thay vì xóa vĩnh viễn ngay lập tức. Soft-delete PHẢI chỉ thay đổi trạng thái hiển thị: mọi liên kết dữ liệu (lịch sử tập, bạn bè, feed, dinh dưỡng) PHẢI được giữ nguyên và chỉ bị ẩn khỏi các bảng khác. Trong 30 ngày, User PHẢI có thể hủy yêu cầu xóa.
- **FR-006**: Hệ thống PHẢI hiển thị danh sách lịch sử buổi tập theo thứ tự thời gian ngược, mỗi mục gồm: ngày tập, thời lượng, số hiệp, tổng khối lượng nâng (kg).
- **FR-007**: Hệ thống PHẢI hiển thị chi tiết 1 buổi tập khi User chọn, gồm danh sách từng bài tập, từng hiệp (reps, weight).
- **FR-008**: Hệ thống PHẢI hỗ trợ phân trang cho lịch sử buổi tập (mặc định 20 buổi/trang) để đảm bảo hiệu năng.
- **FR-009**: WHEN User chọn "tạo plan mới ngay", hệ thống PHẢI chuyển ngay plan cũ sang trạng thái archived và kích hoạt plan mới (active).
- **FR-010**: WHEN User chọn "bắt đầu từ ngày mai", hệ thống PHẢI giữ plan cũ có hiệu lực đến hết ngày hôm nay và lên lịch kích hoạt plan mới từ ngày mai.
- **FR-011**: WHEN User hủy yêu cầu xóa tài khoản trong vòng 30 ngày kể từ khi soft-delete, hệ thống PHẢI khôi phục tài khoản về trạng thái hoạt động với toàn bộ dữ liệu nguyên vẹn (chỉ đổi lại trạng thái, không tạo tài khoản mới).
- **FR-012**: WHEN quá 30 ngày kể từ khi soft-delete mà User không hủy yêu cầu xóa, hệ thống PHẢI hard-delete vĩnh viễn tài khoản và dữ liệu liên quan.

### Key Entities

- **User Profile**: Thông tin cá nhân mở rộng của người dùng (tên hiển thị, ảnh đại diện, tuổi, cân nặng snapshot, chiều cao, mục tiêu, account_status). Liên kết 1-1 với bảng `users`.
- **Workout Session History View**: Tập hợp dữ liệu từ `workout_sessions` và `workout_sets`, hiển thị dưới dạng nhật ký thời gian.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% thông tin hồ sơ (tên, ảnh, tuổi, cân nặng, chiều cao, mục tiêu) hiển thị chính xác khi User truy cập trang hồ sơ.
- **SC-002**: 100% dữ liệu không hợp lệ bị từ chối với thông báo lỗi cụ thể, không có dữ liệu sai được lưu vào hệ thống.
- **SC-003**: User có thể xem chi tiết bất kỳ buổi tập nào trong lịch sử trong vòng 2 lần chạm (1 chạm vào Lịch sử, 1 chạm vào buổi tập).
- **SC-004**: Khi User thay đổi mục tiêu, 100% trường hợp đều hiển thị cảnh báo mất tiến trình tập của ngày hôm nay kèm 2 lựa chọn (tạo plan mới ngay / bắt đầu từ ngày mai).
- **SC-005**: 100% tài khoản soft-delete được hủy yêu cầu xóa trong vòng 30 ngày được khôi phục với toàn bộ dữ liệu nguyên vẹn.
- **SC-006**: 100% tài khoản soft-delete quá 30 ngày không hủy yêu cầu xóa được hard-delete vĩnh viễn, không còn dữ liệu tồn đọng.

## Assumptions

- User đã đăng nhập và có session hợp lệ (JWT token) trước khi truy cập hồ sơ.
- Ảnh đại diện được lưu trữ dưới dạng URL (giải quyết vấn đề storage ở phase triển khai).
- Lịch sử buổi tập sử dụng dữ liệu từ bảng `workout_sessions` và `workout_sets` đã có trong Data Model hiện tại.
- Phân trang mặc định 20 buổi/trang, sắp xếp theo `start_time` giảm dần.
- Ranh giới "hết ngày hôm nay" khi chọn "bắt đầu từ ngày mai" được tính theo múi giờ hệ thống.
