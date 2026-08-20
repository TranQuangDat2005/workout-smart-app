# Feature Specification: Xã hội & Cộng đồng

**Feature Branch**: `003-social-community`

**Created**: 2026-08-17

**Status**: Draft

**Input**: User description: "UC-14: Kết bạn & theo dõi người dùng khác; UC-15: Xếp hạng thi đấu (Leaderboard) — Nhóm 6: Xã hội / Cộng đồng"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Kết bạn & theo dõi người dùng khác (Priority: P1)

Người dùng muốn tìm kiếm và kết bạn với người dùng khác trong hệ thống. Khi gửi lời mời kết bạn, người nhận có thể chấp nhận hoặc từ chối. Nếu chấp nhận, quan hệ hai chiều được thiết lập và cả hai thấy hoạt động của nhau trên feed.

**Why this priority**: Kết nối xã hội là yếu tố giúp duy trì động lực tập luyện (social accountability). Quan hệ bạn bè là nền tảng cho UC-15 (Leaderboard nhóm bạn).

**Independent Test**: Có thể kiểm thử bằng cách: User A tìm kiếm User B → gửi lời mời → User B chấp nhận → xác nhận cả hai hiển thị trong danh sách bạn bè của nhau.

**Acceptance Scenarios**:

1. **Given** User đã đăng nhập, **When** User tìm kiếm người dùng khác theo tên/username, **Then** hệ thống trả danh sách kết quả phù hợp.
2. **Given** User đang xem kết quả tìm kiếm, **When** User nhấn "Kết bạn" trên hồ sơ người khác, **Then** hệ thống kiểm tra rate limit (tối đa 5 lời mời/ngày) và gửi lời mời kết bạn. Nếu lời mời đang pending hoặc đang trong cooldown (30 ngày sau khi bị từ chối), hệ thống chặn và thông báo.
3. **Given** Người nhận có lời mời kết bạn, **When** Người nhận nhấn "Chấp nhận", **Then** hệ thống thiết lập quan hệ hai chiều và cả hai thấy hoạt động của nhau trên feed (mặc định feed chỉ hiển thị cho bạn bè).
4. **Given** Người nhận có lời mời kết bạn, **When** Người nhận nhấn "Từ chối", **Then** hệ thống từ chối lời mời mà không thông báo cho người gửi.
5. **Given** User đã có bạn bè, **When** User mở feed hoạt động, **Then** hệ thống hiển thị hoạt động gần đây của bạn bè (hoàn thành buổi tập, streak mới, v.v.).
6. **Given** Hai User đang là bạn bè, **When** User A nhấn "Hủy kết bạn", **Then** hệ thống xóa quan hệ bạn bè hai chiều, ngừng hiển thị feed của nhau và cập nhật lại leaderboard nhóm bạn.
7. **Given** 2 User gửi lời mời kết bạn cho nhau gần như đồng thời, **When** hệ thống nhận lời mời của bên thứ hai, **Then** hệ thống không tạo lời mời thứ 2 đối xứng: bên nhấn trước (timestamp sớm hơn) giữ vai trò người gửi; phía còn lại, nút "Kết bạn" tự động chuyển thành "Chấp nhận / Từ chối" lời mời đang chờ.

---

### User Story 2 - Xếp hạng thi đua (Leaderboard) (Priority: P2)

Hệ thống tự động xếp hạng thi đua cho mỗi User dựa trên duy nhất một tiêu chí: **Streak** (chuỗi tuần liên tiếp có từ 3 buổi tập trở lên). Bảng xếp hạng là **kỳ thi DÀI VÔ TẬN** (không reset theo chu kỳ): User đang giữ chuỗi tuần dài nhất đứng đầu bảng. User có thể xem bảng xếp hạng toàn server và riêng trong nhóm bạn bè. Bảng xếp hạng chỉ hiển thị Tên hiển thị (display_name) và Thứ hạng.

**Why this priority**: Leaderboard tạo tính cạnh tranh lành mạnh, khuyến khích duy trì thói quen tập luyện thông qua streak.

**Independent Test**: Có thể kiểm thử bằng cách: 2 User hoàn thành số buổi tập khác nhau → vào Xếp hạng → xác nhận thứ tự đúng dựa trên streak tuần.

**Acceptance Scenarios**:

1. **Given** Có nhiều User cùng tham gia, **When** User vào mục "Xếp hạng", **Then** hệ thống hiển thị bảng xếp hạng gồm: thứ hạng, tên hiển thị, streak hiện tại. Không hiển thị các thông tin nhạy cảm khác.
2. **Given** User đang xem bảng xếp hạng, **When** User chuyển giữa tab "Toàn server" và "Nhóm bạn bè", **Then** hệ thống hiển thị xếp hạng tương ứng.
3. **Given** Một User không đạt đủ 3 buổi tập trong tuần, **When** tuần mới bắt đầu, **Then** streak của User đó bị reset về 0 trên leaderboard.
4. **Given** Bảng xếp hạng là kỳ thi vô tận, **When** tuần mới bắt đầu, **Then** hệ thống KHÔNG reset bảng xếp hạng theo chu kỳ; thứ hạng chỉ thay đổi theo streak hiện tại của từng User.
5. **Given** User đang xem bảng xếp hạng, **When** User cuộn danh sách, **Then** vị trí cá nhân của User luôn được highlight hoặc ghim ở cuối bảng.

---

### User Story 3 - Thử thách có thời hạn (Challenge) (Priority: P2)

Admin có thể tạo Thử thách (Challenge) có thời hạn (ví dụ "Thử thách 180 ngày Cutting") để người dùng tham gia thi đua. Khi Challenge kết thúc, hệ thống tự động tổng kết, tính xếp hạng cuối cùng cho người tham gia và lưu lịch sử xếp hạng.

**Why this priority**: Challenge có thời hạn tạo động lực thi đua theo đợt, bổ sung cho kỳ thi streak vô tận, nhưng không làm ảnh hưởng đến leaderboard chính.

**Independent Test**: Có thể kiểm thử bằng cách: Admin tạo Challenge 7 ngày → 2 User tham gia → hệ thống kết thúc Challenge đúng hạn → xác nhận final_rank được lưu cho từng người tham gia.

**Acceptance Scenarios**:

1. **Given** Admin đang ở màn hình quản trị, **When** Admin tạo Challenge (tên, loại mục tiêu, số ngày), **Then** hệ thống tạo challenge với start_date, end_date và status (upcoming/active/completed).
2. **Given** Challenge đang active, **When** User nhấn "Tham gia", **Then** hệ thống ghi nhận User vào challenge_participants (challenge_id, user_id, joined_at).
3. **Given** Challenge đến end_date, **When** hệ thống tổng kết, **Then** hệ thống tính completed_at và final_rank cho từng người tham gia, lưu lịch sử xếp hạng và chuyển status sang completed.

---

### Edge Cases

- Điều gì xảy ra khi 2 User gửi lời mời kết bạn cho nhau gần như đồng thời (lời mời chéo)? (Chỉ 1 lời mời từ bên nhấn trước được tạo — xem FR-011.)
- Điều gì xảy ra khi User block/huỷ kết bạn?
- Điều gì xảy ra khi chỉ có 1 User trong hệ thống — leaderboard hiển thị gì?
- Điều gì xảy ra khi 2 User có cùng streak (tie-breaking)?
- Điều gì xảy ra khi User tham gia Challenge rồi bỏ dở giữa chừng?
- Điều gì xảy ra khi người lạ xem hồ sơ private?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI cho phép User tìm kiếm người dùng khác theo tên hoặc username.
- **FR-002**: WHEN User nhấn "Kết bạn", hệ thống PHẢI áp dụng giới hạn (tối đa 5 lời mời/ngày), chặn nếu đã pending, và áp dụng cooldown 30 ngày nếu đã bị từ chối trước đó. Nếu hợp lệ, tạo lời mời kết bạn (trạng thái pending) và thông báo cho người nhận.
- **FR-003**: WHEN người nhận chấp nhận lời mời, hệ thống PHẢI thiết lập quan hệ hai chiều (friendship) và cập nhật feed cho cả hai (feed mặc định friends-only).
- **FR-004**: WHEN người nhận từ chối lời mời, hệ thống PHẢI xóa lời mời mà không thông báo cho người gửi.
- **FR-004b**: WHEN User nhấn "Hủy kết bạn", hệ thống PHẢI xóa quan hệ hai chiều và loại bỏ các hoạt động tương ứng khỏi feed/leaderboard của nhau.
- **FR-005**: Hệ thống PHẢI hiển thị feed hoạt động của bạn bè (buổi tập hoàn thành, streak, thành tích mới).
- **FR-006**: Hệ thống PHẢI tính xếp hạng dựa trên Streak (chuỗi tuần liên tiếp có >= 3 buổi tập). Nếu 1 tuần < 3 buổi tập, streak reset về 0.
- **FR-007**: Hệ thống PHẢI hiển thị bảng xếp hạng theo 2 phạm vi: toàn server và nhóm bạn bè (extend UC-14), chỉ hiển thị Tên hiển thị và Thứ hạng.
- **FR-008**: Hệ thống PHẢI duy trì bảng xếp hạng streak như một kỳ thi DÀI VÔ TẬN (không reset theo chu kỳ): WHEN streak của User thay đổi, hệ thống PHẢI cập nhật current_streak_weeks, longest_streak_weeks, rank và updated_at; User đang giữ chuỗi tuần dài nhất đứng đầu.
- **FR-009**: Hệ thống PHẢI highlight vị trí cá nhân của User trên bảng xếp hạng.
- **FR-010**: Hệ thống PHẢI xử lý tie-breaking khi nhiều User cùng streak (ưu tiên: thời gian duy trì sớm hơn).
- **FR-011**: WHEN 2 User gửi lời mời kết bạn cho nhau gần như đồng thời, hệ thống PHẢI chỉ tạo một lời mời duy nhất từ bên có timestamp sớm hơn (bên nhấn trước là người gửi); WHERE phía còn lại có lời mời pending từ đối phương, hệ thống PHẢI chuyển nút "Kết bạn" thành "Chấp nhận / Từ chối" lời mời đang chờ (không tạo lời mời thứ 2 đối xứng).
- **FR-012**: WHERE hồ sơ User ở chế độ private, hệ thống PHẢI chỉ cho người lạ (không phải bạn bè) xem display_name và rank, KHÔNG hiển thị bài đăng trên tường cá nhân. WHERE bài đăng được đánh dấu public, hệ thống PHẢI cho mọi người xem bài đăng đó.
- **FR-012b**: Hệ thống CHỈ cho phép bài đăng cộng đồng chứa nội dung dạng ảnh (image) — KHÔNG hỗ trợ video, GIF, hoặc file media khác. Ảnh upload lưu SeaweedFS self-hosted qua backend proxy.
- **FR-013**: WHEN Admin tạo Challenge (name, goal_type, duration_days), hệ thống PHẢI lưu challenge với start_date, end_date và status (upcoming/active/completed).
- **FR-014**: WHEN Challenge đang active, hệ thống PHẢI cho phép User tham gia và ghi nhận vào challenge_participants (challenge_id, user_id, joined_at).
- **FR-015**: WHEN Challenge đến end_date, hệ thống PHẢI tự động tổng kết: tính completed_at và final_rank cho từng người tham gia, lưu lịch sử xếp hạng và chuyển status sang completed.

### Key Entities

- **Friendship**: Quan hệ hai chiều giữa 2 User (user_id_1, user_id_2, status: pending/accepted/rejected, created_at).
- **Friend Request**: Lời mời kết bạn (sender_id, receiver_id, status, timestamp).
- **Leaderboard Entry**: Bản ghi xếp hạng trong kỳ thi vô tận (user_id, current_streak_weeks, longest_streak_weeks, rank, updated_at).
- **Challenge**: Thử thách có thời hạn do Admin tạo (name, goal_type, duration_days, start_date, end_date, status).
- **Challenge Participant**: Người tham gia Challenge (challenge_id, user_id, joined_at, completed_at, final_rank).
- **Activity Feed Item**: Hoạt động của User hiển thị cho bạn bè (user_id, action_type, details, timestamp).
- **Community Post**: Bài đăng cộng đồng của User (user_id, content_text, image_url, visibility: public/friends_only, created_at). Chỉ hỗ trợ ảnh, KHÔNG hỗ trợ video.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: User có thể tìm kiếm và gửi lời mời kết bạn trong vòng 15 giây (tìm → nhấn Kết bạn).
- **SC-002**: Bảng xếp hạng hiển thị chính xác và cập nhật trong vòng 5 phút sau khi User hoàn thành buổi tập.
- **SC-003**: 100% Challenge được tổng kết tự động đúng thời hạn (ngay khi đến end_date) và lưu lịch sử xếp hạng (final_rank) đầy đủ cho mọi người tham gia.
- **SC-004**: Feed hoạt động hiển thị sự kiện mới của bạn bè trong vòng 1 phút sau khi sự kiện xảy ra.

## Assumptions

- Mỗi User có thể có tối đa 500 bạn bè (giới hạn hợp lý cho hiệu năng feed).
- Streak có định nghĩa duy nhất toàn hệ thống: chuỗi tuần liên tiếp có >= 3 buổi tập; bỏ 1 tuần < 3 buổi tập thì streak reset về 0.
- Leaderboard là kỳ thi vô tận, tính thuần túy dựa trên streak tuần; không có kỳ thi đua kết thúc/reset điểm theo chu kỳ.
- Challenge do Admin tạo thủ công (không tự sinh định kỳ).
- Feed hoạt động chỉ hiển thị sự kiện trong 7 ngày gần nhất.
- Leaderboard cập nhật theo batch (mỗi 5 phút) thay vì realtime để tối ưu hiệu năng.
