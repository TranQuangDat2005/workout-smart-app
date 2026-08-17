# Tasks: Xác thực & Quản lý tài khoản (007-core-auth)

**Input**: Design documents from `/specs/007-core-auth/`

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/openapi.yaml ✅

**Tests**: BẮT BUỘC theo constitution §9 (coverage ≥80% service + integration happy/error path).

**Organization**: Tasks nhóm theo user story để từng story implement/test độc lập.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Khởi tạo 3 project rỗng theo stack constitution §2

- [ ] T001 Khởi tạo backend Spring Boot 3.3 + Java 17 + Maven tại `backend/` (pom.xml với spring-boot-starter-web, security, data-jpa, validation, flyway-core, jjwt 0.12.x, springdoc-openapi, postgresql driver, lombok)
- [ ] T002 [P] Tạo `backend/src/main/resources/application.yml` (datasource PostgreSQL 16, JPA, Flyway enable, cấu hình JWT secret/expiry qua env, server port)
- [ ] T003 [P] Khởi tạo web React 18 + TypeScript strict + Vite tại `web/` (eslint, prettier, react-router-dom, axios) — tuân thủ DESIGN.md
- [ ] T004 [P] [DEFERRED] Khởi tạo mobile Flutter tại `mobile/` — thực hiện SAU khi web hoàn thành

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Hạ tầng bắt buộc trước mọi user story

**⚠️ CRITICAL**: Không story nào bắt đầu được khi phase này chưa xong

- [ ] T005 Tạo Flyway migration `backend/src/main/resources/db/migration/V1__init.sql` — bảng `users` đủ cột theo data-model.md §1 (email unique, password_hash, account_status, email_verified, deleted_at...)
- [ ] T006 Tạo Flyway migration `backend/src/main/resources/db/migration/V2__auth_tables.sql` — bảng `otp_verifications`, `refresh_tokens` + index theo data-model.md §2-3
- [ ] T007 [P] Entity `User` tại `backend/src/main/java/com/workoutsmart/auth/entity/User.java`
- [ ] T008 [P] Entity `OtpVerification` tại `backend/src/main/java/com/workoutsmart/auth/entity/OtpVerification.java`
- [ ] T009 [P] Entity `RefreshToken` tại `backend/src/main/java/com/workoutsmart/auth/entity/RefreshToken.java`
- [ ] T010 Repository `UserRepository` tại `backend/src/main/java/com/workoutsmart/auth/repository/UserRepository.java` (findByEmail, existsByEmail)
- [ ] T011 [P] Repository `OtpVerificationRepository` tại `backend/src/main/java/com/workoutsmart/auth/repository/OtpVerificationRepository.java` (findLatestByUserIdAndPurpose)
- [ ] T012 [P] Repository `RefreshTokenRepository` tại `backend/src/main/java/com/workoutsmart/auth/repository/RefreshTokenRepository.java` (findByTokenHash, revokeAllByUserId)
- [ ] T013 `JwtProvider` tại `backend/src/main/java/com/workoutsmart/auth/security/JwtProvider.java` — ký/verify access (HS256, 15 phút) theo research R1
- [ ] T014 `SecurityConfig` + `BanCheckFilter` tại `backend/src/main/java/com/workoutsmart/auth/security/` — filter check `account_status` (cache 60s) trả 403 khi banned (research R5)
- [ ] T015 `GlobalExceptionHandler` tại `backend/src/main/java/com/workoutsmart/auth/exception/GlobalExceptionHandler.java` — map lỗi 400/401/403/409/422/429 theo constitution §3
- [ ] T016 [P] Interface `EmailService` tại `backend/src/main/java/com/workoutsmart/auth/service/EmailService.java` + impl `LogEmailService` (dev, ghi OTP ra log) + impl `SendGridEmailService` (production, API key từ env) theo research R4

**Checkpoint**: Foundation ready — các story có thể bắt đầu

---

## Phase 3: User Story 1 - Đăng ký & xác thực email (Priority: P1) 🎯 MVP

**Goal**: User đăng ký email/mật khẩu → nhận OTP → verify → tài khoản active; email soft-delete <30 ngày → luồng restore.

**Independent Test**: register → 201 + OTP; verify-otp → 200; register lại email cũ → 409.

### Tests for User Story 1 ⚠️

- [ ] T017 [P] [US1] Unit test `OtpService` tại `backend/src/test/java/com/workoutsmart/auth/service/OtpServiceTest.java` (sinh OTP, hash, verify sai/hết hạn/5 lần)
- [ ] T018 [P] [US1] Unit test `AuthService.register/verify` tại `backend/src/test/java/com/workoutsmart/auth/service/AuthServiceTest.java`
- [ ] T019 [US1] Integration test register + verify tại `backend/src/test/java/com/workoutsmart/auth/controller/AuthControllerIT.java` (happy + 409 + 400)

### Implementation for User Story 1

- [ ] T020 [P] [US1] DTO `RegisterRequest`, `VerifyOtpRequest` tại `backend/src/main/java/com/workoutsmart/auth/dto/`
- [ ] T021 [US1] `OtpService` tại `backend/src/main/java/com/workoutsmart/auth/service/OtpService.java` — sinh OTP 6 số, bcrypt hash, TTL 10 phút, max 5 lần sai, cooldown 60s, ≤3 lần/giờ (research R3)
- [ ] T022 [US1] `AuthService.register` + `AuthService.verifyOtp` tại `backend/src/main/java/com/workoutsmart/auth/service/AuthService.java` — tạo account chờ xác thực, gửi OTP, verify kích hoạt; email soft-delete → OTP purpose=restore (FR-010)
- [ ] T023 [US1] Endpoint `POST /api/v1/auth/register`, `POST /api/v1/auth/verify-otp`, `POST /api/v1/auth/resend-otp` tại `backend/src/main/java/com/workoutsmart/auth/controller/AuthController.java` + Bean Validation
- [ ] T024 [US1] Endpoint `POST /api/v1/auth/restore-password` (đặt mật khẩu mới khi restore — research R6)
- [ ] T025 [US1] Màn hình web Register + VerifyOtp tại `web/src/pages/auth/` + `web/src/services/authApi.ts`
- [ ] T026 [US1] [DEFERRED] Màn hình mobile Register + VerifyOtp tại `mobile/lib/features/auth/` + `mobile/lib/services/auth_api.dart`

**Checkpoint**: US1 hoạt động độc lập end-to-end

---

## Phase 4: User Story 2 - Đăng nhập & phiên (Priority: P1)

**Goal**: Login cấp access + refresh; silent refresh rotation; logout; ban chặn ngay (FR-012..014).

**Independent Test**: login → 200 + 2 token; refresh → cặp mới, token cũ revoked; ban → request kế tiếp 403.

### Tests for User Story 2 ⚠️

- [ ] T027 [P] [US2] Unit test `TokenService` tại `backend/src/test/java/com/workoutsmart/auth/service/TokenServiceTest.java` (rotation, revoke, hết hạn)
- [ ] T028 [US2] Integration test login/refresh/logout/ban tại `backend/src/test/java/com/workoutsmart/auth/controller/AuthControllerIT.java` (happy + 401/403/429)

### Implementation for User Story 2

- [ ] T029 [P] [US2] DTO `LoginRequest`, `RefreshRequest` tại `backend/src/main/java/com/workoutsmart/auth/dto/`
- [ ] T030 [US2] `TokenService` tại `backend/src/main/java/com/workoutsmart/auth/service/TokenService.java` — cấp access + refresh (SHA-256 hash lưu DB, 7 ngày), rotation, revoke theo research R2
- [ ] T031 [US2] `AuthService.login`, `AuthService.refresh`, `AuthService.logout` tại `backend/src/main/java/com/workoutsmart/auth/service/AuthService.java` — từ chối banned/chưa verify (FR-006), rate limit 5 lần sai/15 phút
- [ ] T032 [US2] Endpoint `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` tại `backend/src/main/java/com/workoutsmart/auth/controller/AuthController.java`
- [ ] T033 [US2] Admin ban → invalidate cache + revoke all refresh tokens (event hook trong `BanCheckFilter`/`AuthService`) — phối hợp interface với 006-admin-management
- [ ] T034 [US2] Web: `web/src/services/tokenStorage.ts` (refresh trong httpOnly cookie, access in-memory), `silentRefresh.ts`, màn hình Login
- [ ] T035 [US2] [DEFERRED] Mobile: `mobile/lib/services/token_storage.dart` (flutter_secure_storage), `silent_refresh.dart`, màn hình Login

**Checkpoint**: US1 + US2 hoạt động độc lập

---

## Phase 5: User Story 3 - Quên & đặt lại mật khẩu (Priority: P2)

**Goal**: Forgot password gửi OTP (không lộ email tồn tại); reset bằng OTP + mật khẩu mới.

**Independent Test**: forgot-password → 200 (ẩn email không tồn tại); reset-password → 200; login mật khẩu mới → 200.

### Tests for User Story 3 ⚠️

- [ ] T036 [US3] Integration test forgot/reset password tại `backend/src/test/java/com/workoutsmart/auth/controller/AuthControllerIT.java` (happy + OTP sai + email không tồn tại vẫn 200)

### Implementation for User Story 3

- [ ] T037 [P] [US3] DTO `ForgotPasswordRequest`, `ResetPasswordRequest` tại `backend/src/main/java/com/workoutsmart/auth/dto/`
- [ ] T038 [US3] `AuthService.forgotPassword` + `AuthService.resetPassword` tại `backend/src/main/java/com/workoutsmart/auth/service/AuthService.java` — OTP purpose=reset_password
- [ ] T039 [US3] Endpoint `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password` tại `backend/src/main/java/com/workoutsmart/auth/controller/AuthController.java`
- [ ] T040 [US3] Web màn hình ForgotPassword + ResetPassword tại `web/src/pages/auth/`
- [ ] T041 [US3] [DEFERRED] Mobile màn hình ForgotPassword + ResetPassword tại `mobile/lib/features/auth/`

**Checkpoint**: Cả 3 story hoạt động độc lập

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Hoàn thiện trước khi merge

- [ ] T042 Cron job hard-delete tài khoản soft-delete quá 30 ngày tại `backend/src/main/java/com/workoutsmart/auth/service/AccountCleanupJob.java` (FR-011)
- [ ] T043 [P] Cập nhật OpenAPI (`specs/007-core-auth/contracts/openapi.yaml` khớp thực tế) + springdoc config
- [ ] T044 Chạy toàn bộ kịch bản `specs/007-core-auth/quickstart.md` và sửa lỗi phát sinh
- [ ] T045 [P] Coverage check: `mvn test` đạt ≥80% cho package auth (jacoco report)
- [ ] T046 Commit theo Conventional Commits + `git flow feature finish 007-core-auth` (merge về develop)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (P1)**: Không phụ thuộc — bắt đầu ngay
- **Foundational (P2)**: Phụ thuộc Setup — CHẶN mọi story
- **US1 (P3)**: Sau Foundational — không phụ thuộc story khác 🎯 MVP
- **US2 (P4)**: Sau Foundational — dùng entity/refresh từ Foundational; độc lập với US1
- **US3 (P5)**: Sau Foundational — tái sử dụng OtpService từ US1 (nên làm sau US1)
- **Polish (P6)**: Sau tất cả story mong muốn

### Within Each User Story

- Tests viết TRƯỚC, chạy FAIL trước khi implement
- Entities (Foundational) → Repositories → Services → Endpoints → Client UI
- Story hoàn thành trước khi chuyển priority tiếp theo

### Parallel Opportunities

- T002/T003/T004 (init 3 project) song song
- T007/T008/T009 (3 entity) song song; T010/T011/T012 (3 repository) song song
- T017/T018 (unit tests US1) song song
- T025/T026 (web/mobile US1) song song sau khi endpoint xong
- US1, US2, US3 chạy song song nếu đủ người (US3 cần OtpService nên khuyến nghị sau US1)

---

## Implementation Strategy

### MVP First (US1 Only)

1. Phase 1 Setup → Phase 2 Foundational → Phase 3 US1
2. **STOP & VALIDATE**: đăng ký + verify chạy end-to-end
3. Demo MVP

### Incremental Delivery

1. Setup + Foundational → nền tảng sẵn sàng
2. US1 → test độc lập → MVP ✅
3. US2 → test độc lập (login/refresh/ban)
4. US3 → test độc lập (quên mật khẩu)
5. Polish → merge develop

## Notes

- [P] = file khác nhau, không phụ thuộc
- Commit sau mỗi task hoặc nhóm logic (Conventional Commits, subject ≤50 ký tự)
- KHÔNG xóa migration; KHÔNG commit secret; JWT secret qua env
- Backend là trọng tâm; web/mobile chỉ luồng auth mỏng gọi API
