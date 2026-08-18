# Feature Specification: Chuẩn hoá UI/UX & Hardening Acceptance

**Feature Branch**: `010-uiux-polish`

**Created**: 2026-08-18

**Status**: Draft

**Input**: User description: "Đóng vai QA thực hiện acceptance test UI/UX, ghi nhận các vấn đề còn tồn đọng vào spec và cập nhật giao diện web để khớp với spec/DESIGN.md."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Phiên đăng nhập liền mạch trên toàn bộ ứng dụng (Priority: P1)

Người dùng đã đăng nhập và đang thao tác (ghi hiệp tập, ghi bữa ăn, xem thống kê...) khi access token hết hạn sau 15 phút. Hệ thống PHẢI tự động gia hạn phiên (silent refresh) rồi thực hiện lại request mà người dùng không bị gián đoạn hay nhìn thấy lỗi.

**Why this priority**: Nếu silent refresh chỉ hoạt động ở một phần request, người dùng sẽ gặp lỗi giữa buổi tập — phá vỡ trải nghiệm và trái với FR-008 của `007-core-auth`.

**Independent Test**: Đăng nhập, đợi access token hết hạn, gửi bất kỳ request dữ liệu nào (tải hồ sơ, lưu hiệp) → xác nhận request vẫn thành công mà không phải đăng nhập lại.

**Acceptance Scenarios**:

1. **Given** Người dùng có refresh token hợp lệ, **When** access token hết hạn và người dùng gửi request dữ liệu, **Then** hệ thống tự động refresh access token và thực hiện lại request gốc thành công.
2. **Given** Cả access token và refresh token đều hết hạn, **When** người dùng gửi request, **Then** hệ thống xoá phiên và đưa người dùng về màn hình đăng nhập.

---

### User Story 2 - Giao diện hiển thị tốt trên mọi kích thước thiết bị (Priority: P1)

Người dùng truy cập web trên desktop, tablet và mobile. Bố cục PHẢI thích ứng: sidebar thu gọn/ẩn trên màn hình nhỏ, nội dung không bị tràn, và điều hướng vẫn dùng được.

**Why this priority**: `DESIGN.md §8` đã quy định breakpoint và chiến lược thu gọn nhưng giao diện hiện tại chưa áp dụng; trên mobile sidebar cố định chiếm gần hết chiều rộng màn hình.

**Independent Test**: Mở app ở độ rộng < 576px → xác nhận sidebar không chiếm cố định 240px, có điều hướng dạng thu gọn/thanh dưới, và các grid xếp dọc thay vì 2 cột.

**Acceptance Scenarios**:

1. **Given** Viewport < 576px, **When** người dùng mở trang được bảo vệ, **Then** điều hướng chính hiển thị ở dạng thu gọn (bottom bar / drawer) thay vì sidebar cố định.
2. **Given** Viewport 576–1024px, **When** người dùng mở các grid 2 cột, **Then** các cột xếp lại theo bố cục phù hợp mà không bị tràn ngang.
3. **Given** Viewport > 1024px, **When** người dùng mở trang, **Then** hiển thị đầy đủ sidebar + nội dung như thiết kế desktop.

---

### User Story 3 - Đồng hồ nghỉ (Rest Timer) trong buổi tập (Priority: P1)

Người dùng đang tập, sau khi nhấn "Lưu hiệp", hệ thống PHẢI tự động bắt đầu đồng hồ đếm ngược thời gian nghỉ, hiển thị số giây còn lại và phát cảnh báo ở 5 giây cuối.

**Why this priority**: Rest Timer là tính năng cốt lõi giải quyết bài toán "nghỉ quá lâu, mất tập trung" theo `009-workout-tracking` FR-001/FR-002.

**Independent Test**: Bắt đầu buổi tập → lưu 1 hiệp → xác nhận đồng hồ nghỉ đếm ngược → xác nhận cảnh báo ở 5 giây cuối.

**Acceptance Scenarios**:

1. **Given** Người dùng vừa lưu 1 hiệp, **When** thao tác lưu thành công, **Then** đồng hồ nghỉ tự động bắt đầu và hiển thị thời gian còn lại.
2. **Given** Đồng hồ nghỉ đang chạy, **When** còn 5 giây cuối, **Then** hệ thống phát tín hiệu cảnh báo (âm thanh/rung/notification trên web).
3. **Given** Đồng hồ nghỉ kết thúc, **When** thời gian về 0, **Then** hệ thống phát tín hiệu kết thúc nghỉ để người dùng tiếp tục.

---

### User Story 4 - Luồng thay đổi mục tiêu và xóa tài khoản an toàn, rõ ràng (Priority: P1)

Khi người dùng thay đổi mục tiêu tập luyện, hệ thống PHẢI cảnh báo rõ ràng về việc mất tiến trình hôm nay và cho 2 lựa chọn (tạo ngay / bắt đầu từ ngày mai). Khi xóa tài khoản, hệ thống PHẢI yêu cầu xác nhận mật khẩu.

**Why this priority**: Tránh hành động phá huỷ dữ liệu do hiểu nhầm, đúng `001-profile-history` FR-004/FR-005.

**Independent Test**: Sửa goal_type trong hồ sơ → xác nhận xuất hiện dialog 2 lựa chọn; nhấn "Xóa tài khoản" → xác nhận yêu cầu nhập mật khẩu.

**Acceptance Scenarios**:

1. **Given** Người dùng sửa goal_type và lưu, **When** mục tiêu khác mục tiêu hiện tại, **Then** hệ thống hiển thị cảnh báo "tạo lại plan sẽ mất tiến trình hôm nay" kèm 2 lựa chọn "Tạo ngay" và "Bắt đầu từ ngày mai".
2. **Given** Người dùng chọn "Tạo ngay", **When** xác nhận, **Then** plan cũ chuyển archived và plan mới active ngay.
3. **Given** Người dùng chọn "Bắt đầu từ ngày mai", **When** xác nhận, **Then** plan cũ giữ hiệu lực hết hôm nay, plan mới kích hoạt từ ngày mai.
4. **Given** Người dùng yêu cầu xóa tài khoản, **When** chưa nhập đúng mật khẩu, **Then** hệ thống KHÔNG xóa và hiển thị lỗi inline.

---

### User Story 5 - Khả năng truy cập và nhất quán ngôn ngữ/trạng thái (Priority: P2)

Người dùng (bao gồm người dùng bàn phím và màn hình đọc) PHẢI nhìn thấy trạng thái focus, các trường nhập có nhãn rõ ràng, và mọi trạng thái hiển thị bằng tiếng Việt nhất quán thay vì giá trị thô tiếng Anh.

**Why this priority**: Cải thiện khả năng sử dụng và tính chuyên nghiệp; giảm nhầm lẫn khi thấy `ACTIVE/BANNED/DELETED`, `open/finished`.

**Independent Test**: Dùng phím Tab di chuyển → xác nhận thấy vòng focus; xem bảng admin/leaderboard → xác nhận trạng thái đã được dịch sang tiếng Việt.

**Acceptance Scenarios**:

1. **Given** Người dùng dùng phím Tab, **When** focus vào button/link/chip, **Then** hệ thống hiển thị vòng focus rõ ràng.
2. **Given** Trang có OTP 6 chữ số, **When** màn hình đọc quét form, **Then** mỗi ô có nhãn riêng (ví dụ "Chữ số 1").
3. **Given** Các bảng hiển thị trạng thái tài khoản/bài tập/thử thách, **When** render, **Then** trạng thái được hiển thị bằng nhãn tiếng Việt thay vì giá trị enum thô.

---

### User Story 6 - Hoàn thiện các luồng phụ (Dinh dưỡng, Xếp hạng, Lịch sử, Quản trị) (Priority: P2)

Người dùng ghi bữa ăn phải thấy đúng tên món và calo/macro. Bảng xếp hạng phải có 2 phạm vi và highlight vị trí cá nhân. Lịch sử tập phải phân trang. Admin phải xem được chi tiết user, sửa bài tập và nhập đủ thumbnail/hướng dẫn.

**Why this priority**: Các yêu cầu này đã có trong spec gốc nhưng giao diện chưa triển khai đủ, gây thiếu hụt chức năng khi acceptance test.

**Independent Test**: Thêm 2 món vào bữa ăn → thấy tên món và tổng macro; mở Xếp hạng → chuyển phạm vi + thấy vị trí mình; mở Lịch sử > 20 buổi → thấy phân trang; Admin mở user → thấy chi tiết.

**Acceptance Scenarios**:

1. **Given** Người dùng thêm món vào bữa ăn, **When** món đã được chọn, **Then** danh sách "Món đã chọn" hiển thị tên món và calo/macro (không hiển thị ID).
2. **Given** Người dùng mở bảng xếp hạng, **When** chuyển tab "Toàn server"/"Nhóm bạn bè", **Then** hiển thị xếp hạng tương ứng và vị trí cá nhân được highlight/ghim.
3. **Given** Lịch sử tập có hơn 20 buổi, **When** người dùng xem danh sách, **Then** có điều khiển phân trang hoặc tải thêm.
4. **Given** Admin xem danh sách user, **When** chọn 1 user, **Then** xem được chi tiết hồ sơ và lịch sử tập.
5. **Given** Admin tạo/sửa bài tập, **When** mở form, **Then** có đủ trường thumbnail (180×180) và hướng dẫn từng bước.

---

### Edge Cases

- Điều gì xảy ra khi nhiều request gửi đồng thời cùng hết hạn token (chỉ refresh 1 lần, không tạo vòng lặp refresh)?
- Điều gì xảy ra khi viewport rất hẹp (< 360px) với nội dung bảng nhiều cột?
- Điều gì xảy ra khi người dùng lưu hiệp nhưng chưa chọn bài tập (rest timer lấy thời gian nghỉ từ đâu)?
- Điều gì xảy ra khi thay đổi mục tiêu nhưng backend không có plan active để archive?
- Điều gì xảy ra khi bữa ăn có món bị xóa khỏi kho cá nhân (tên món trong danh sách món đã chọn/lịch sử)?
- Điều gì xảy ra khi lịch sử tập trống nhưng người dùng vẫn mở phân trang?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: THE hệ thống SHALL dùng một HTTP client dùng chung cho mọi request từ Web, gắn access token và tự động silent refresh (dùng refresh token) khi nhận 401 trước khi thực hiện lại request gốc; WHEN refresh token cũng hết hạn, THE hệ thống SHALL xoá phiên và chuyển về màn hình đăng nhập.
- **FR-002**: THE hệ thống SHALL áp dụng breakpoint responsive theo `DESIGN.md §8`; WHERE viewport < 576px, THE điều hướng chính SHALL thu gọn (bottom bar hoặc drawer) thay vì sidebar cố định; các grid nhiều cột SHALL xếp lại phù hợp, không tràn ngang.
- **FR-003**: WHEN người dùng lưu một hiệp tập thành công, THE hệ thống SHALL tự động bắt đầu đồng hồ nghỉ đếm ngược và hiển thị thời gian còn lại; WHILE còn 5 giây cuối, THE hệ thống SHALL phát cảnh báo; WHEN về 0, THE hệ thống SHALL phát tín hiệu kết thúc nghỉ.
- **FR-004**: WHEN người dùng thay đổi goal_type sang giá trị khác, THE hệ thống SHALL hiển thị cảnh báo mất tiến trình hôm nay và yêu cầu chọn MỘT trong HAI: (1) tạo plan mới ngay, hoặc (2) plan mới bắt đầu từ ngày mai; sau lựa chọn, THE hệ thống SHALL thực hiện archive/kích hoạt tương ứng.
- **FR-005**: WHEN người dùng yêu cầu xóa tài khoản, THE hệ thống SHALL yêu cầu xác nhận mật khẩu; chỉ khi xác nhận đúng mới chuyển trạng thái soft-delete.
- **FR-006**: THE hệ thống SHALL hiển thị vòng focus rõ ràng cho các phần tử tương tác (button, link, chip, input) khi điều hướng bằng bàn phím.
- **FR-007**: THE hệ thống SHALL cung cấp nhãn truy cập cho từng ô OTP (ví dụ "Chữ số 1..6").
- **FR-008**: THE hệ thống SHALL hiển thị mọi trạng thái (tài khoản, bài tập, thử thách, session) bằng nhãn tiếng Việt nhất quán, không hiển thị enum thô tiếng Anh.
- **FR-009**: WHEN người dùng thêm món vào bữa ăn, THE hệ thống SHALL hiển thị tên món, khẩu phần và calo/macro của từng món cùng tổng bữa (không hiển thị ID nội bộ).
- **FR-010**: THE hệ thống SHALL hiển thị bảng xếp hạng theo 2 phạm vi (Toàn server / Nhóm bạn bè) và highlight/ghim vị trí cá nhân của người dùng.
- **FR-011**: THE hệ thống SHALL hỗ trợ phân trang (hoặc tải thêm) cho lịch sử buổi tập, mặc định 20 buổi/trang.
- **FR-012**: THE hệ thống SHALL cho phép Admin xem chi tiết hồ sơ và lịch sử tập của người dùng; form tạo/sửa bài tập SHALL có đủ trường thumbnail (180×180) và hướng dẫn từng bước.
- **FR-013**: THE hệ thống SHALL thay thế các hộp thoại trình duyệt thô (`confirm`/`prompt`) bằng thành phần dialog của design system để nhất quán trải nghiệm.

### Key Entities

Không thêm entity dữ liệu mới. Thay đổi chủ yếu ở tầng trình bày web (client) và một vài trường hiển thị sử dụng lại entity hiện có (`users`, `exercises`, `workout_sessions`, `food_items`, `meal_logs`, `leaderboard_entries`, `challenges`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% request dữ liệu tự động gia hạn phiên thành công khi access token hết hạn (không hiện lỗi 401 cho người dùng).
- **SC-002**: Giao diện không bị tràn ngang và điều hướng dùng được trên các breakpoint < 576px, 576–1024px, > 1024px.
- **SC-003**: 100% lần lưu hiệp thành công đều kích hoạt đồng hồ nghỉ; cảnh báo 5 giây cuối phát đúng.
- **SC-004**: 100% lần thay đổi goal_type đều hiển thị dialog 2 lựa chọn; xóa tài khoản 100% yêu cầu mật khẩu.
- **SC-005**: 100% trạng thái hiển thị bằng tiếng Việt; các phần tử tương tác có focus rõ ràng; OTP có nhãn từng ô.
- **SC-006**: Người dùng hoàn tất ghi 1 bữa ăn có nhiều món và thấy đúng tên + tổng macro trong vòng 30 giây.
- **SC-007**: Lịch sử > 20 buổi có thể duyệt hết; Admin xem được chi tiết user và sửa được bài tập với đủ trường.

## Assumptions

- Chỉ áp dụng cho Web (React 18 + TypeScript); Mobile Flutter triển khai sau.
- Các yêu cầu chức năng gốc (Rest Timer, đổi mục tiêu, xóa tài khoản, leaderboard, admin) đã có trong `001/003/006/009`; feature này tập trung đóng các gap ở tầng giao diện và hạ tầng HTTP client.
- API backend đã hỗ trợ `effectiveDate`, `warning`, `goalChanged`, phân trang và endpoint admin cần thiết; phần này chỉ cần nối đúng từ client.
- Breakpoint và chiến lược thu gọn lấy từ `DESIGN.md §8`.
- Không thay đổi invariant đã chốt trong constitution (streak, retention, sync, account lifecycle).
