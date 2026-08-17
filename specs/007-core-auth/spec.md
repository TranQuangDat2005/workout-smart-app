# Feature Specification: Xác thực & Quản lý tài khoản

**Feature Branch**: `007-core-auth`

**Created**: 2026-08-17

**Status**: APPROVED

**Input**: User description: "UC-01: Đăng ký tài khoản; UC-02: Xác thực OTP; UC-03: Đăng nhập; UC-20: Quên mật khẩu / Reset Password — Nhóm 1: Core & Auth"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Đăng ký tài khoản & xác thực email (Priority: P1)

Người dùng mới muốn tạo tài khoản bằng email và mật khẩu. Sau khi đăng ký, hệ thống gửi mã OTP qua email để xác thực địa chỉ email trước khi kích hoạt tài khoản. Điều này đảm bảo email hợp lệ và giảm tài khoản rác.

**Why this priority**: Đăng ký là cổng vào duy nhất của hệ thống (không có Guest user). Mọi tính năng cá nhân hóa (lộ trình, thống kê, xã hội) đều phụ thuộc vào tài khoản đã xác thực.

**Independent Test**: Có thể kiểm thử độc lập bằng cách điền form đăng ký (email + mật khẩu) → nhận mã OTP → nhập mã → tài khoản được kích hoạt và có thể đăng nhập.

**Acceptance Scenarios**:

1. **Given** Người dùng chưa có tài khoản, **When** người dùng nhập email hợp lệ và mật khẩu đạt yêu cầu rồi nhấn "Đăng ký", **Then** hệ thống tạo tài khoản ở trạng thái chờ xác thực và gửi mã OTP đến email.
2. **Given** Người dùng đã nhận mã OTP, **When** người dùng nhập đúng mã trong thời hạn, **Then** tài khoản được kích hoạt và người dùng được chuyển đến bước thiết lập mục tiêu.
3. **Given** Email đã được đăng ký trước đó (tài khoản không ở trạng thái soft-delete), **When** người dùng cố đăng ký lại bằng email đó, **Then** hệ thống từ chối kèm thông báo "Email đã tồn tại".
4. **Given** Người dùng nhập mật khẩu yếu hoặc email sai định dạng, **When** người dùng nhấn "Đăng ký", **Then** hệ thống hiển thị lỗi inline và không tạo tài khoản.
5. **Given** Tài khoản đã bị soft-delete chưa quá 30 ngày, **When** người dùng đăng ký lại bằng đúng email đó, **Then** hệ thống khôi phục tài khoản cũ với toàn bộ dữ liệu nguyên vẹn (chỉ đổi lại trạng thái, không tạo tài khoản mới) và hướng dẫn người dùng đăng nhập.

---

### User Story 2 - Đăng nhập (Priority: P1)

Người dùng đã có tài khoản muốn đăng nhập để truy cập các tính năng cá nhân. Hệ thống xác thực mật khẩu và cấp token phiên đăng nhập để duy trì trạng thái mà không cần đăng nhập lại ở mỗi thao tác.

**Why this priority**: Đăng nhập là điều kiện tiên quyết để truy cập toàn bộ ứng dụng. Không có đăng nhập, không thể sử dụng bất kỳ tính năng nào.

**Independent Test**: Có thể kiểm thử bằng cách nhập đúng email/mật khẩu → xác nhận đăng nhập thành công và truy cập được hồ sơ cá nhân.

**Acceptance Scenarios**:

1. **Given** Người dùng có tài khoản đã kích hoạt, **When** người dùng nhập đúng email và mật khẩu, **Then** hệ thống cấp phiên đăng nhập hợp lệ và chuyển vào màn hình chính.
2. **Given** Người dùng nhập sai mật khẩu, **When** người dùng nhấn "Đăng nhập", **Then** hệ thống từ chối kèm thông báo chung "Email hoặc mật khẩu không đúng".
3. **Given** Tài khoản chưa xác thực email, **When** người dùng cố đăng nhập, **Then** hệ thống nhắc hoàn tất xác thực OTP trước khi cho phép truy cập.
4. **Given** Tài khoản bị khóa (banned), **When** người dùng cố đăng nhập, **Then** hệ thống từ chối truy cập và thông báo tài khoản đã bị khóa.
5. **Given** Người dùng đang có phiên đăng nhập hợp lệ (token còn hạn), **When** Admin ban tài khoản và người dùng gửi request tiếp theo, **Then** hệ thống vô hiệu hóa token ngay và từ chối request — kể cả khi người dùng bị ban trong lúc offline, request đầu tiên khi kết nối lại cũng bị từ chối và người dùng buộc phải đăng nhập lại.

---

### User Story 3 - Quên mật khẩu & đặt lại mật khẩu (Priority: P2)

Người dùng quên mật khẩu muốn đặt lại mật khẩu mới một cách an toàn. Hệ thống gửi mã OTP qua email để xác thực quyền sở hữu tài khoản trước khi cho phép đặt mật khẩu mới.

**Why this priority**: Khôi phục truy cập là luồng thiết yếu cho giữ chân người dùng, nhưng không phải điều kiện tiên quyết của lần đăng nhập đầu tiên.

**Independent Test**: Có thể kiểm thử bằng cách nhấn "Quên mật khẩu" → nhập email → nhận OTP → nhập mã + mật khẩu mới → đăng nhập thành công bằng mật khẩu mới.

**Acceptance Scenarios**:

1. **Given** Người dùng quên mật khẩu, **When** người dùng nhập email đã đăng ký và yêu cầu đặt lại, **Then** hệ thống gửi mã OTP đến email (không tiết lộ email có tồn tại hay không).
2. **Given** Người dùng nhận được OTP, **When** người dùng nhập đúng mã và mật khẩu mới hợp lệ, **Then** hệ thống cập nhật mật khẩu và cho phép đăng nhập lại.
3. **Given** OTP hết hạn hoặc sai, **When** người dùng nhập mã không hợp lệ, **Then** hệ thống từ chối và cho phép yêu cầu gửi lại mã mới.

---

### Edge Cases

- Điều gì xảy ra khi người dùng yêu cầu gửi lại OTP quá nhiều lần (rate limiting)?
- Điều gì xảy ra khi OTP hết hạn giữa lúc nhập mã?
- Điều gì xảy ra khi phiên đăng nhập hết hạn trong lúc người dùng đang thao tác?
- Điều gì xảy ra khi email OTP bị rơi vào thư mục spam?
- Điều gì xảy ra khi người dùng đăng ký lại bằng email của tài khoản đang soft-delete?
- Điều gì xảy ra khi tài khoản bị ban trong lúc người dùng đang offline?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI cho phép người dùng đăng ký tài khoản bằng email và mật khẩu; mật khẩu PHẢI được lưu dưới dạng mã hóa một chiều (không lưu mật khẩu thô).
- **FR-002**: Hệ thống PHẢI validate email đúng định dạng và mật khẩu đạt độ mạnh tối thiểu trước khi tạo tài khoản.
- **FR-003**: WHEN người dùng đăng ký thành công, hệ thống PHẢI tạo tài khoản ở trạng thái chờ xác thực và gửi mã OTP qua email.
- **FR-004**: WHEN người dùng nhập đúng OTP trong thời hạn, hệ thống PHẢI kích hoạt tài khoản và chuyển người dùng đến bước thiết lập mục tiêu.
- **FR-005**: WHEN người dùng nhập đúng email/mật khẩu, hệ thống PHẢI xác thực mật khẩu và cấp phiên đăng nhập (access token + refresh token).
- **FR-006**: Hệ thống PHẢI từ chối đăng nhập khi mật khẩu sai, tài khoản chưa xác thực, hoặc tài khoản bị khóa, với thông báo phù hợp.
- **FR-007**: WHEN người dùng quên mật khẩu, hệ thống PHẢI gửi OTP qua email để xác thực và cho phép đặt lại mật khẩu mới.
- **FR-008**: Hệ thống PHẢI cho phép tự động gia hạn phiên đăng nhập (silent refresh) khi access token hết hạn, dùng refresh token còn hiệu lực.
- **FR-009**: WHEN refresh token cũng hết hạn, hệ thống PHẢI đưa người dùng về màn hình đăng nhập.
- **FR-010**: WHEN người dùng đăng ký lại bằng đúng email của tài khoản đang ở trạng thái soft-delete trong vòng 30 ngày, hệ thống PHẢI khôi phục tài khoản cũ với toàn bộ dữ liệu nguyên vẹn (chỉ đổi lại trạng thái, không tạo tài khoản mới).
- **FR-011**: Tài khoản soft-delete PHẢI giữ nguyên mọi liên kết dữ liệu (lịch sử tập, bạn bè, feed, dinh dưỡng) và chỉ bị ẩn khỏi các bảng khác trong 30 ngày; WHEN quá 30 ngày, hệ thống PHẢI hard-delete vĩnh viễn tài khoản.
- **FR-012**: WHEN Admin ban tài khoản, hệ thống PHẢI revoke ngay access token và refresh token của tài khoản đó.
- **FR-013**: WHEN một request đi qua middleware, hệ thống PHẢI kiểm tra trạng thái ban của tài khoản và PHẢI từ chối mọi request của tài khoản bị ban, kể cả request có token còn hạn.
- **FR-014**: Lệnh ban PHẢI có hiệu lực ngay khi người dùng kết nối lại online — request đầu tiên sau khi kết nối lại của tài khoản bị ban PHẢI bị từ chối, kể cả khi tài khoản bị ban trong lúc đang offline.

### Key Entities

- **User Account**: Tài khoản người dùng gồm email, mật khẩu (đã mã hóa), `email_verified` (boolean — false trước khi xác thực OTP, true sau khi xác thực), `account_status` (active/banned/deleted), vai trò (user/admin). Trạng thái "chờ xác thực" được biểu diễn qua `email_verified = false` kết hợp `account_status = active`.
- **OTP Verification**: Mã xác thực dùng một lần gắn với mục đích (đăng ký/đặt lại mật khẩu) và thời hạn hiệu lực.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Người dùng hoàn tất đăng ký + xác thực email trong vòng 2 phút.
- **SC-002**: 100% mật khẩu được lưu mã hóa, không có mật khẩu thô tồn tại trong hệ thống.
- **SC-003**: Người dùng đăng nhập thành công trong vòng 10 giây khi nhập đúng thông tin.
- **SC-004**: 95% người dùng hoàn tất đặt lại mật khẩu thành công ở lần thử đầu tiên.

## Assumptions

- Hệ thống có tích hợp dịch vụ gửi email (SendGrid/AWS SES) để gửi OTP.
- OTP có thời hạn hiệu lực cố định (mặc định 10 phút) và giới hạn số lần gửi lại.
- Đăng nhập áp dụng cho cả Web và Mobile với cùng cơ chế token.
- Social login (Google/Facebook/Apple) nằm ngoài scope phase này — chỉ hỗ trợ email/password.
