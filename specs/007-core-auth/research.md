# Research: Xác thực & Quản lý tài khoản (007-core-auth)

**Feature**: specs/007-core-auth | **Date**: 2026-08-17

Tất cả các điểm NEEDS CLARIFICATION trong Technical Context đã được nghiên cứu và chốt. Mỗi mục gồm Decision / Rationale / Alternatives.

## R1. Thư viện JWT cho Spring Boot 3.3

- **Decision**: `jjwt` (0.12.x) cho ký/verify JWT; dùng `HMAC-SHA256` (HS256) cho access token, `HS512` cho refresh token.
- **Rationale**: nhẹ, không phụ thuộc framework, được cộng đồng dùng rộng rãi với Spring Boot 3.x; thao tác ký/verify đơn giản cho cả access lẫn refresh. Refresh token được lưu hash trong DB nên không cần JWE/JWS phức tạp.
- **Alternatives**: `nimbus-jose-jwt` (mạnh hơn, hỗ trợ JWE — nhưng thừa cho nhu cầu này); `spring-security-oauth2-resource-server` (cần authorization server riêng — quá nặng cho MVP).

## R2. Chiến lược token: access 15 phút + refresh 7 ngày + rotation

- **Decision**: Access token JWT vô trạng thái (HS256, 15 phút). Refresh token là chuỗi ngẫu nhiên 256-bit, chỉ lưu **SHA-256 hash** trong bảng `refresh_tokens`; mỗi lần refresh → **rotation** (revoke token cũ, cấp token mới). Mỗi refresh token gắn `jti` claim trong access để trace.
- **Rationale**: rotation + lưu hash cho phép **revoke ngay khi ban** (FR-012) và phát hiện token bị replay (dùng lại refresh đã xoay → revoke cả chuỗi). Access ngắn nên không cần denylist.
- **Alternatives**: JWT thuần vô trạng thái cho cả 2 token (không thể revoke khi ban → vi phạm FR-012); lưu token thô trong DB (rò rỉ DB = mất tài khoản); access token dài (giảm số lần refresh nhưng tăng cửa sổ rủi ro khi bị ban).

## R3. OTP qua email: sinh, lưu, giới hạn

- **Decision**: OTP 6 chữ số, TTL **10 phút** (khớp assumption trong spec). Lưu **bcrypt hash** của OTP trong bảng `otp_verifications` kèm `purpose` (register/reset_password), `expires_at`, `attempts_count`. Giới hạn: tối đa **5 lần nhập sai** → OTP vô hiệu; tối đa **3 lần gửi/giờ/email**; cooldown **60 giây** giữa 2 lần gửi.
- **Rationale**: bcrypt-hash chống lộ OTP khi DB bị đọc; giới hạn attempt chống brute-force 6 chữ số (không gian 10⁶, 5 lần thử là an toàn). Cooldown + cap chống spam email.
- **Alternatives**: lưu OTP thô (rủi ro khi DB leak); TTL dài hơn (tăng cửa sổ tấn công); không giới hạn resend (tốn phí email + spam).

## R4. Email provider: SendGrid vs AWS SES

- **Decision**: Định nghĩa interface `EmailService` (sendOtp(email, code, purpose)). Default implementation **SendGrid**; AWS SES là implementation thứ 2 có thể swap qua config. Email OTP template chung: mã 6 số + thời hạn 10 phút + cảnh báo không chia sẻ mã.
- **Rationale**: cả 2 đều nằm trong allowlist của AGENTS.md; interface trừu tượng giúp đổi provider không đụng business code; đây là quyết định vận hành (chọn 1 khi deploy), không phải kiến trúc.
- **Alternatives**: SMTP tự dựng (phải quản lý rate limit/reputation — không đáng cho MVP); gọi trực tiếp SDK không qua interface (khóa cứng provider).

## R5. Middleware kiểm tra trạng thái ban trên mọi request

- **Decision**: Filter (Spring Security `OncePerRequestFilter`) chạy sau JWT authentication: đọc `account_status` của user. Dùng **cache ngắn 60 giây** cho status (để không query DB mỗi request) + **invalidate cache ngay khi Admin ban** (qua event/sau khi 006-admin cập nhật DB). Nếu status = banned → trả 403, đồng thời xóa/revoke refresh tokens của user (FR-012).
- **Rationale**: đáp ứng "hiệu lực ngay khi kết nối lại online, chặn ≤3 giây ở request kế tiếp" (FR-014) mà vẫn giữ hiệu năng (P95 < 300ms). Cache 60s nghĩa là user bị ban offline thì request đầu tiên sau khi online bị chặn chắc chắn (cache đã hết hạn).
- **Alternatives**: query DB mỗi request (chính xác tuyệt đối nhưng thêm 1 query/request); chỉ kiểm tra lúc login (vi phạm FR-013 — token cũ vẫn hoạt động); dùng denylist JWT (tốn bộ nhớ, phức tạp với access token ngắn).

## R6. Khôi phục tài khoản soft-delete qua đăng ký lại

- **Decision**: Khi email đăng ký khớp tài khoản `account_status = deleted` và `deleted_at` chưa quá 30 ngày: KHÔNG tạo account mới. Gửi OTP mục đích `restore`; sau khi verify OTP → đổi `account_status = active`, giữ nguyên `password_hash` cũ... **KHÔNG** — quyết định cuối: user phải đặt **mật khẩu mới** trong luồng khôi phục (OTP xác nhận sở hữu email → form đặt mật khẩu mới → account active trở lại, toàn bộ dữ liệu liên kết giữ nguyên vì chưa bao giờ bị xóa cứng).
- **Rationale**: soft-delete giữ nguyên mọi liên kết dữ liệu (FR-011) nên "khôi phục" chỉ là đổi state. Bắt đặt mật khẩu mới an toàn hơn giữ hash cũ (không ai biết mật khẩu cũ sau thời gian xóa). Verify OTP chống kẻ lạ đăng ký email đang soft-delete để chiếm tài khoản người cũ.
- **Alternatives**: khôi phục thẳng không cần OTP (lỗ hổng chiếm tài khoản); giữ mật khẩu cũ (đơn giản nhưng kém an toàn); chặn đăng ký email soft-delete đến khi hết 30 ngày (gây khó chịu cho chính chủ).

## R7. Lưu token phía client (Web & Mobile)

- **Decision**: Mobile (Flutter): lưu cả access + refresh trong `flutter_secure_storage` (Keychain/Keystore). Web (React): refresh token trong **httpOnly + Secure cookie** (không cho JS đọc), access token giữ trong memory (React state/closure) — mất access khi reload thì gọi silent refresh qua cookie. Silent refresh dùng refresh token → access mới; khi nhận 401/403 từ API thì chuyển về màn hình đăng nhập.
- **Rationale**: httpOnly cookie + access-in-memory chống XSS đọc token trên web; secure storage là chuẩn trên mobile. Khớp FR-008/FR-009 (silent refresh, refresh hết hạn → kick về login).
- **Alternatives**: localStorage cho cả 2 (đơn giản nhưng XSS đọc được); memory-only cho refresh web (reload mất session — UX kém).

## R8. Password policy & chuẩn hóa email

- **Decision**: Password tối thiểu **8 ký tự, chứa ≥1 chữ hoa, ≥1 chữ thường, ≥1 chữ số** (FR-002 "độ mạnh tối thiểu"). Email chuẩn hóa: `trim` + `lowercase` trước khi lưu/tìm; unique constraint trên cột email.
- **Rationale**: đủ mạnh cho MVP mà không gây khó chịu (không bắt ký tự đặc biệt); chuẩn hóa tránh đăng ký trùng `User@x.com` / `user@x.com`.
- **Alternatives**: NIST-style chỉ cần độ dài ≥12 (ít rào cản hơn nhưng phổ thông người dùng VN quen 8 ký tự); bắt ký tự đặc biệt (khó nhớ, không tăng bảo mật đáng kể).

## R9. Rate limiting endpoint auth

- **Decision**: Rate limit nhẹ tại gateway/filter: đăng ký & quên mật khẩu & resend-OTP ≤ **10 request/phút/IP**; login ≤ **5 lần thử sai/15 phút/tài khoản** (sau đó khóa tạm 15 phút, trả 429).
- **Rationale**: chặn brute-force mật khẩu + spam email mà không cần hạ tầng phức tạp (bảng/counter trong memory hoặc Redis nếu sau này scale).
- **Alternatives**: không rate limit (rủi ro brute-force); dùng hẳn Redis/Bucket4j (thêm dependency — cân nhắc khi scale, chưa cần cho MVP).
