# Feature Specification: Nhạc luyện tập qua URL (017-workout-music)

**Feature Branch**: `017-workout-music`

**Created**: 2026-08-19

**Status**: Draft

**Input**: User description: "Thêm tự động phát nhạc bằng URL ở phần luyện tập." Clarify đã chốt: nguồn YouTube/Spotify (Q1=B), lưu localStorage (Q3=A), mini player đầy đủ play/pause/volume/seek (Q4=B), tự dừng khi kết thúc buổi tập.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dán URL nhạc ở màn bắt đầu buổi tập (Priority: P1)

Màn "Sẵn sàng chinh phục?" có thêm 1 ô nhập URL nhạc (tùy chọn): YouTube, Spotify hoặc file mp3 trực tiếp. URL hợp lệ được lưu vào `localStorage` và khôi phục khi mở lại sau.

**Why this priority**: Không có URL thì không có nhạc — đây là điểm vào duy nhất của feature.

**Independent Test**: Mở tab Hôm nay (chưa có session) → thấy ô nhập → dán URL YouTube → bấm Bắt đầu → reload trang → URL vẫn còn trong ô.

**Acceptance Scenarios**:

1. **Given** chưa có buổi tập active, **When** mở tab Hôm nay, **Then** hệ thống hiển thị ô "Nhạc luyện tập (URL YouTube/Spotify/mp3 — tùy chọn)" với giá trị từ `localStorage` nếu có.
2. **Given** User dán URL hợp lệ và bấm "Bắt đầu buổi tập", **Then** hệ thống lưu URL vào `localStorage` và khởi tạo player.
3. **Given** User dán URL không hợp lệ và bấm Bắt đầu, **Then** buổi tập vẫn bắt đầu bình thường, hệ thống hiển thị lỗi nhẹ "URL nhạc không hợp lệ" và bỏ qua nhạc (FR-006).

---

### User Story 2 - Tự động phát khi bắt đầu buổi tập (Priority: P1)

Khi bấm "Bắt đầu buổi tập" với URL hợp lệ, hệ thống tự phát nhạc. Nếu trình duyệt chặn autoplay, hiển thị nút play để User bật.

**Why this priority**: "Tự động phát" là yêu cầu cốt lõi của feature.

**Independent Test**: Dán URL mp3 → Bắt đầu buổi → nhạc phát ngay; kết thúc buổi → nhạc dừng.

**Acceptance Scenarios**:

1. **Given** URL mp3 hợp lệ, **When** User bấm "Bắt đầu buổi tập", **Then** hệ thống phát nhạc ngay trong gesture đó.
2. **Given** URL YouTube hợp lệ, **When** player sẵn sàng, **Then** hệ thống gọi phát tự động; nếu trình duyệt chặn, nút play hiển thị rõ để User bấm.
3. **Given** URL Spotify hợp lệ, **When** hiển thị, **Then** hệ thống nhúng Spotify embed với điều khiển gốc của Spotify.

---

### User Story 3 - Mini player trong buổi tập (Priority: P1)

Khi buổi tập đang chạy và có nhạc, hệ thống hiển thị thanh mini player: play/pause, âm lượng, thanh seek (kéo thời gian) cho mp3 và YouTube. Nhạc tiếp tục phát khi nghỉ giữa hiệp (beep dùng kênh audio riêng, không xung đột).

**Why this priority**: User cần điều khiển nhạc mà không rời màn tập.

**Independent Test**: Bắt đầu buổi với URL mp3 → thấy thanh player → bấm pause → nhạc dừng → kéo seek → nhạc nhảy đúng vị trí → chỉnh volume → âm lượng đổi.

**Acceptance Scenarios**:

1. **Given** player mp3/YouTube đang chạy, **When** bấm play/pause, **Then** nhạc dừng/tiếp tục đúng trạng thái.
2. **Given** player đang chạy, **When** kéo thanh volume, **Then** âm lượng thay đổi theo.
3. **Given** player đang chạy, **When** kéo thanh seek, **Then** nhạc nhảy đến đúng vị trí thời gian.
4. **Given** đang nghỉ giữa hiệp, **When** đếm ngược nghỉ chạy, **Then** nhạc vẫn phát và beep 5s/0s vẫn kêu rõ.

---

### User Story 4 - Tự dừng khi kết thúc buổi tập (Priority: P2)

Khi buổi tập kết thúc (complete/expired) hoặc user kết thúc sớm, hệ thống tự dừng và hủy player.

**Why this priority**: Nhạc phát mãi sau khi tập xong là trải nghiệm tệ; dừng tự động đóng vòng lặp.

**Independent Test**: Đang phát nhạc → bấm "Kết thúc buổi tập" → nhạc dừng, player biến mất.

**Acceptance Scenarios**:

1. **Given** nhạc đang phát, **When** User kết thúc buổi tập, **Then** nhạc dừng và player không còn hiển thị.
2. **Given** buổi tập expired sang ngày mới, **When** hệ thống báo lỗi 409, **Then** player dừng cùng trạng thái session.

---

### Edge Cases

- URL YouTube dạng watch/shorts/youtu.be/embed — tất cả phải parse được ra video id.
- URL Spotify dạng web link lẫn URI (`spotify:track:...`) — parse ra được path embed.
- URL chết/403 — hiển thị lỗi nhẹ, không chặn luồng tập.
- User đổi URL giữa 2 buổi — buổi mới dùng URL mới; buổi đang chạy giữ player cũ.
- Reload giữa buổi — URL khôi phục từ `localStorage`; player khởi tạo lại từ đầu (mất vị trí phát — chấp nhận).
- Không có URL — không hiển thị player, mọi thứ hoạt động như trước.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: WHEN chưa có buổi tập active, THE client SHALL hiển thị ô nhập URL nhạc (tùy chọn) ở màn bắt đầu buổi tập; giá trị SHALL được lưu vào `localStorage` và khôi phục khi mở lại.
- **FR-002**: THE client SHALL parse URL hỗ trợ 3 loại: mp3/ogg/m4a/wav trực tiếp, YouTube (watch/shorts/youtu.be/embed), Spotify (web link/URI).
- **FR-003**: WHEN User bấm "Bắt đầu buổi tập" WHERE có URL hợp lệ, THE client SHALL khởi tạo player và phát nhạc ngay; WHERE trình duyệt chặn autoplay, client SHALL hiển thị nút play rõ ràng. Với YouTube, client SHALL chỉ phát ÂM THANH — iframe video SHALL bị ẩn khỏi giao diện (không hiển thị phần xem video).
- **FR-004**: WHEN buổi tập đang chạy, THE client SHALL hiển thị mini player với play/pause, volume và seek cho mp3/YouTube; Spotify SHALL dùng embed với điều khiển gốc.
- **FR-005**: WHEN nghỉ giữa hiệp, nhạc SHALL tiếp tục phát; beep nghỉ SHALL dùng kênh audio riêng (Web Audio) không xung đột.
- **FR-006**: WHEN URL không hợp lệ hoặc không phát được, THE client SHALL hiển thị lỗi nhẹ và KHÔNG chặn luồng tập.
- **FR-007**: WHEN buổi tập kết thúc (complete/expired/kết thúc sớm), THE client SHALL tự dừng và hủy player.
- **FR-008**: THE client KHÔNG được gọi API điều khiển nhạc từ backend; mọi phát nhạc diễn ra phía trình duyệt theo URL do User cung cấp.

### Key Entities

- **Không có entity backend** — feature thuần Web client; dữ liệu duy nhất là URL trong `localStorage` (key `workoutMusicUrl`).
- **UI components (web)**: `WorkoutMusicPlayer` (mini player), `parseMusicSource` (parse URL thuần túy).

## Success Criteria *(mandatory)*

- **SC-001**: 100% URL hợp lệ thuộc 3 loại được parse đúng và player khởi tạo đúng loại.
- **SC-002**: Với mp3/YouTube, 3 thao tác play/pause, volume, seek hoạt động.
- **SC-003**: Nhạc tự dừng khi kết thúc buổi tập (không phát vãng lai).
- **SC-004**: URL lưu `localStorage` và khôi phục sau reload.
- **SC-005**: Không có URL → màn hình không đổi so với trước feature (không hiện player rỗng).

## Assumptions

- Chỉ Web client — mobile (Flutter) chưa có feature này (triển khai sau web).
- Spotify không có API điều khiển (play/pause/seek) miễn phí → dùng điều khiển gốc của embed; mini player đầy đủ chỉ áp dụng mp3 + YouTube.
- Autoplay tuân theo policy trình duyệt; gesture "Bắt đầu buổi tập" được dùng để mở khóa phát.
- Nhạc là nội dung do User tự cung cấp — app không lưu trữ, không phân phối, không kiểm tra bản quyền.
- **Governance**: allowlist external API được mở rộng cho YouTube IFrame Player API + Spotify embed (chỉ Web client) theo phê duyệt của owner 2026-08-19 — xem constitution §5 và AGENTS.md §2.
