# Quickstart Validation: Xác thực & Quản lý tài khoản (007-core-auth)

Hướng dẫn kiểm chứng feature chạy đúng end-to-end. Chi tiết kỹ thuật xem [data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml). Code thực thi nằm trong `tasks.md` (bước tiếp theo `/speckit-tasks`).

## Prerequisites

- PostgreSQL 16 chạy local (hoặc docker), đã chạy Flyway migration V1 + V2 (bảng `users`, `otp_verifications`, `refresh_tokens`).
- Backend Spring Boot: `cd backend && ./mvnw spring-boot:run`
- Email: chạy MailHog (dev) hoặc stub `EmailService` ghi OTP ra log để lấy mã trong test.
- Web: `cd web && npm run dev` · Mobile: `flutter run` (tùy theo client đang kiểm).

## Các kịch bản kiểm chứng

### 1. Đăng ký + xác thực OTP (User Story 1)
1. Gọi `POST /api/v1/auth/register` `{email, password}` → expect **201**; DB: `users.email_verified=false`, `otp_verifications` có row `purpose=register`.
2. Lấy OTP 6 số từ MailHog/log → `POST /api/v1/auth/verify-otp` → expect **200**; `users.email_verified=true`.
3. Đăng ký lại cùng email → expect **409** "Email đã tồn tại".

### 2. Đăng nhập + refresh + logout (User Story 2)
1. `POST /api/v1/auth/login` đúng email/password → expect **200** + access (15') + refresh (7 ngày).
2. Gọi 1 API có auth với access token → **200**. Sau khi access hết hạn → `POST /api/v1/auth/refresh` → cặp token mới; refresh token cũ bị revoked (dùng lại → **401**).
3. `POST /api/v1/auth/logout` → refresh token bị revoked; refresh lại → **401**.
4. Login sai mật khẩu 5 lần → **429** trong 15 phút.

### 3. Ban giữa phiên (FR-012..FR-014)
1. User đang có access token hợp lệ. Admin ban user (bảng `users.account_status=banned`).
2. Request tiếp theo của user → **403**, kể cả token còn hạn. Đăng nhập lại → bị từ chối. Tất cả `refresh_tokens` của user bị `revoked_at` set.

### 4. Quên mật khẩu (User Story 3)
1. `POST /api/v1/auth/forgot-password` với email tồn tại → **200**, OTP gửi. Với email không tồn tại → vẫn **200**, không gửi gì.
2. `POST /api/v1/auth/reset-password` với OTP + mật khẩu mới → **200**; login bằng mật khẩu mới → **200**.

### 5. Khôi phục tài khoản soft-delete (FR-010)
1. User A xóa tài khoản (`account_status=deleted`). Đăng ký lại đúng email → **201** với OTP `purpose=restore` (không tạo account mới).
2. Verify OTP → đặt mật khẩu mới (`/auth/restore-password`) → account **active**, toàn bộ dữ liệu liên kết còn nguyên.
3. Lặp lại sau 30 ngày (`deleted_at` quá hạn, giả lập bằng cron) → đăng ký tạo tài khoản **mới** bình thường.

## Lệnh test tự động

```bash
# Backend (≥80% coverage phần auth)
cd backend && ./mvnw test

# Web
cd web && npm test

# Mobile
cd mobile && flutter test
```

## Expected outcomes (tóm tắt)

| Tiêu chí | Kỳ vọng |
|---|---|
| SC-001 | Đăng ký + verify email hoàn tất < 2 phút |
| SC-002 | Không có mật khẩu thô trong DB (chỉ bcrypt hash) |
| SC-003 | Login thành công < 10 giây |
| SC-004 | ≥95% reset password thành công lần đầu |
