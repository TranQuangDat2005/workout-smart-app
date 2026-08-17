# Data Model: Xác thực & Quản lý tài khoản (007-core-auth)

**Feature**: specs/007-core-auth | **Date**: 2026-08-17

## Tổng quan

3 bảng liên quan feature này: `users` (mở rộng từ General Spec — thêm cột xác thực), `otp_verifications` (mới), `refresh_tokens` (mới). Migration Flyway: `V2__auth_tables.sql` (bảng `users` cơ bản nằm ở `V1__init.sql`).

## 1. Bảng `users` (mở rộng)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Chuẩn hóa lowercase + trim trước khi lưu |
| `password_hash` | VARCHAR(100) | NOT NULL | bcrypt |
| `role` | VARCHAR(20) | NOT NULL, default 'user' | user/admin |
| `goal_type` | VARCHAR(30) | NULL | weight_loss/muscle_gain/endurance (điền sau ở UC-04) |
| `fitness_level` | VARCHAR(30) | NULL | beginner/intermediate/advanced |
| `sex` | VARCHAR(10) | NULL | male/female — cần cho TDEE (UC-12) |
| `activity_level` | VARCHAR(20) | NULL | sedentary/light/moderate/active/very_active |
| `display_name` | VARCHAR(100) | NULL | |
| `avatar_url` | VARCHAR(500) | NULL | |
| `age` | INT | NULL | |
| `weight_kg` | NUMERIC(5,2) | NULL | snapshot từ body_metrics |
| `height_cm` | NUMERIC(5,2) | NULL | |
| `account_status` | VARCHAR(20) | NOT NULL, default 'active' | active / banned / deleted |
| `email_verified` | BOOLEAN | NOT NULL, default false | false = chờ xác thực OTP |
| `deleted_at` | TIMESTAMPTZ | NULL | set khi soft-delete; +30 ngày → cron hard-delete |
| `created_at` | TIMESTAMPTZ | NOT NULL | |
| `updated_at` | TIMESTAMPTZ | NOT NULL | |

### State transitions (`account_status`)

```
(đăng ký) → active + email_verified=false
   └─ verify OTP → active + email_verified=true
active ── Admin ban ──→ banned ── Admin unban ──→ active
active ── User xóa ──→ deleted (set deleted_at)
deleted ── đăng ký lại + OTP + mật khẩu mới (<30 ngày) ──→ active
deleted ── cron sau 30 ngày ──→ hard-delete (xóa row + dữ liệu liên kết)
banned ── KHÔNG thể tự khôi phục (chỉ Admin)
```

Quy tắc nghiệp vụ:
- Đăng nhập chỉ thành công khi `account_status = active` và `email_verified = true`.
- `deleted` không hiển thị ở mọi bảng liên kết (bạn bè, leaderboard...) — xử lý ở tầng query của từng feature.
- Middleware ban-check đọc `account_status` (cache 60s, invalidate khi ban).

## 2. Bảng `otp_verifications` (mới)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK → users.id, NOT NULL | |
| `purpose` | VARCHAR(30) | NOT NULL | register / reset_password / restore |
| `code_hash` | VARCHAR(100) | NOT NULL | bcrypt hash của OTP 6 số |
| `expires_at` | TIMESTAMPTZ | NOT NULL | now + 10 phút |
| `attempts_count` | INT | NOT NULL, default 0 | ≥5 → vô hiệu |
| `used_at` | TIMESTAMPTZ | NULL | set khi verify thành công |
| `created_at` | TIMESTAMPTZ | NOT NULL | |

Quy tắc:
- Mỗi lần gửi OTP → tạo row mới (không update row cũ). Verify luôn check row **mới nhất** theo purpose.
- Verify: `used_at IS NULL` + `expires_at > now` + `attempts_count < 5` + bcrypt match → đánh dấu `used_at`.
- Rate limit (research R3): ≤3 lần gửi/giờ/email, cooldown 60s giữa các lần gửi.

## 3. Bảng `refresh_tokens` (mới)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|---|---|---|---|
| `id` | BIGSERIAL | PK | |
| `user_id` | BIGINT | FK → users.id, NOT NULL, INDEX | |
| `token_hash` | VARCHAR(64) | UNIQUE, NOT NULL | SHA-256 của refresh token |
| `expires_at` | TIMESTAMPTZ | NOT NULL | now + 7 ngày |
| `revoked_at` | TIMESTAMPTZ | NULL | set khi logout/rotation/ban |
| `created_at` | TIMESTAMPTZ | NOT NULL | |

Quy tắc (research R2):
- Refresh: tìm theo `token_hash`, check `revoked_at IS NULL` + `expires_at > now` → revoke row cũ + tạo row mới (rotation).
- Ban user → revoke TẤT CẢ refresh tokens của user đó (FR-012).
- Replay detection: dùng refresh token đã revoked → revoke cả chuỗi của user (tùy chọn hardening).

## 4. Quan hệ

```text
users 1 ──── n otp_verifications
users 1 ──── n refresh_tokens
```

Không thêm quan hệ nào khác trong feature này. `users.role` sẽ được tái sử dụng bởi 006-admin-management.

## 5. Migration impact

- `V1__init.sql` (đã dự kiến trong General Spec): toàn bộ bảng gốc, gồm `users` với đủ cột trong bảng trên.
- `V2__auth_tables.sql`: `otp_verifications` + `refresh_tokens` + index (`otp_verifications(user_id, purpose, created_at)`, `refresh_tokens(user_id)`).
- Không sửa migration đã chạy (constitution §7).
