# Implementation Plan: Xác thực & Quản lý tài khoản (Core & Auth)

**Branch**: `feature/007-core-auth` | **Date**: 2026-08-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/007-core-auth/spec.md`

## Summary

Xây dựng hệ thống xác thực cho cả 3 client (Web React, Mobile Flutter, Admin portal sau này): đăng ký bằng email/mật khẩu với xác thực OTP qua email, đăng nhập cấp JWT (access 15 phút + refresh 7 ngày), silent refresh, quên/đặt lại mật khẩu qua OTP, khôi phục tài khoản soft-delete trong 30 ngày, và cơ chế ban/revoke token hiệu lực ngay qua middleware.

Backend (Spring Boot 3.3) là trái tim của feature này: toàn bộ endpoint auth + middleware kiểm tra trạng thái ban. Web/Mobile chỉ tiêu thụ API (màn hình đăng ký/đăng nhập/quên mật khẩu + lưu token an toàn + silent refresh).

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3), TypeScript strict (React 18), Dart (Flutter)

**Primary Dependencies**: Spring Security, Spring Web, Spring Data JPA, Jakarta Validation, thư viện JWT (jjwt — xem research.md), SendGrid / AWS SES / SMTP Gmail (qua interface EmailService), Flyway, springdoc-openapi

**Storage**: PostgreSQL 18 — bảng `users` (đã có trong General Spec), thêm `otp_verifications`, `refresh_tokens`

**Testing**: JUnit 5 + Mockito (backend ≥80% coverage), Jest + React Testing Library (web), flutter test (mobile)

**Target Platform**: Backend Linux server; Web browser; Mobile iOS/Android

**Project Type**: web-service + 2 client apps (web SPA + mobile)

**Performance Goals**: API auth < 300ms (P95); đăng nhập thành công trong < 10 giây theo cảm nhận người dùng (SC-003); middleware ban-check không làm chậm đáng kể request thường (lookup PK + cache ngắn)

**Constraints**: bcrypt cho mật khẩu; OTP 10 phút hiệu lực; không commit secret; email provider nằm trong allowlist (SendGrid / AWS SES / SMTP Gmail); mọi request qua middleware phải check trạng thái ban (FR-013/014)

**Scale/Scope**: MVP — hàng nghìn user; 3 màn hình web + 3 màn hình mobile; 8 endpoint auth; 3 bảng dữ liệu (1 bảng mở rộng `users` + 2 bảng mới)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Nguyên tắc (Constitution v1.0.0) | Đánh giá |
|---|---|---|
| 1 | Layered: Controller → Service → Repository → Entity | ✅ Tuân thủ — AuthController mỏng, logic trong AuthService/TokenService/OtpService |
| 2 | Bean Validation cho DTO; lỗi tập trung: 400/401/403/409/422 | ✅ Tuân thủ — ghi rõ trong contracts |
| 3 | Không raw SQL; JPA + Flyway; không xóa migration | ✅ Tuân thủ — migration mới cho `otp_verifications`, `refresh_tokens` |
| 4 | JWT + bcrypt; middleware check ban mọi request; revoke token khi ban | ✅ Tuân thủ — là yêu cầu cốt lõi FR-012/013/014 |
| 5 | Email trong allowlist (SendGrid / AWS SES / SMTP Gmail) | ✅ Tuân thủ — interface EmailService, provider swap được |
| 6 | Audit log cho thao tác Admin | ✅ N/A trong feature này (ban thuộc 006-admin-management, auth chỉ thực thi hiệu lực) |
| 7 | Test ≥80% coverage service + integration happy/error | ✅ Tuân thủ — đưa vào DoD |
| 8 | OpenAPI cập nhật cho mọi endpoint | ✅ Tuân thủ — contracts/openapi.yaml |
| 9 | Git Flow: feature/007-core-auth, Conventional Commits | ✅ Tuân thủ |
| 10 | EARS trong spec | ✅ Spec đã APPROVED |

**Kết quả GATE**: PASS — không vi phạm. (Re-check sau Phase 1: vẫn PASS, xem cuối file.)

## Project Structure

### Documentation (this feature)

```text
specs/007-core-auth/
├── plan.md              # File này
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── contracts/
│   └── openapi.yaml     # Phase 1 output — API auth endpoints
├── quickstart.md        # Phase 1 output
├── checklists/
│   └── requirements.md
├── spec.md
└── tasks.md             # Phase 2 (/speckit-tasks — CHƯA tạo)
```

### Source Code (repository root)

```text
backend/                                  # Spring Boot 3.3 (Maven) — deliverable chính
├── src/main/java/com/workoutsmart/
│   ├── auth/
│   │   ├── controller/    # AuthController
│   │   ├── dto/           # RegisterRequest, LoginRequest, VerifyOtpRequest, ...
│   │   ├── service/       # AuthService, OtpService, TokenService, EmailService
│   │   ├── entity/        # User, OtpVerification, RefreshToken
│   │   ├── repository/    # UserRepository, OtpVerificationRepository, RefreshTokenRepository
│   │   ├── security/      # JwtProvider, BanCheckFilter, SecurityConfig
│   │   └── exception/     # GlobalExceptionHandler
│   └── ...
├── src/main/resources/
│   ├── db/migration/      # Flyway: V1__init.sql, V2__auth_tables.sql
│   └── application.yml
└── src/test/java/...      # unit + integration

web/                                      # React 18 + TS strict + Vite
├── src/
│   ├── pages/auth/        # Register, Login, ForgotPassword, VerifyOtp
│   ├── services/          # authApi, tokenStorage, silentRefresh
│   └── ...
└── tests/

mobile/                                   # Flutter
├── lib/
│   ├── features/auth/     # register, login, forgot_password
│   ├── services/          # auth_api, token_storage (flutter_secure_storage), silent_refresh
│   └── ...
└── test/
```

**Structure Decision**: Monorepo 3 app (`backend/`, `web/`, `mobile/`) theo đúng stack đã chốt trong constitution. Feature này backend là trọng tâm; web/mobile chỉ xây luồng UI auth mỏng gọi API.

## Complexity Tracking

> Không có vi phạm constitution cần biện minh.

## Constitution Re-check (post Phase 1)

Sau khi thiết kế xong (research.md + data-model.md + contracts):

- ✅ Middleware ban-check: đọc `account_status` qua PK lookup + cache ngắn (60s) — thỏa FR-013/014 "hiệu lực ngay" mà không phá vỡ nguyên tắc hiệu năng.
- ✅ Revoke refresh token: bảng `refresh_tokens` cho phép revoke ngay khi ban + rotation khi refresh — thỏa FR-012.
- ✅ Khôi phục soft-delete yêu cầu OTP xác nhận quyền sở hữu email trước khi đổi state — tránh lỗ hổng chiếm tài khoản (security).
- ✅ Không vi phạm nguyên tắc nào — GATE PASS.
