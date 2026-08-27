# WorkoutSmartApp Constitution

<!--
  Sync Impact Report
  Version change: 1.0.0 → 2.0.0
  Modified principles:
  - Immutable Tech Stack: PostgreSQL 16 → PostgreSQL 18 (khớp container thực tế đang chạy).
  Templates requiring updates: không.
  Runtime guidance updated: AGENTS.md (đã đồng bộ).
  Follow-up TODOs: không.

  Version change: 2.0.0 → 2.1.0 (2026-08-19)
  Modified principles:
  - §5 External API allowlist: thêm YouTube IFrame Player API + Spotify embed
    (nhạc luyện tập — chỉ Web client, URL do User cung cấp, không gọi từ backend).
    Phê duyệt: owner (chọn Q1=B trong clarify 017-workout-music).
  Templates requiring updates: không.
  Runtime guidance updated: AGENTS.md (đã đồng bộ).
  Follow-up TODOs: không.

  Version change: 2.1.0 → 2.2.0 (2026-08-19)
  Modified principles:
  - §4 TDEE: mục tiêu calo chuyển từ tỷ lệ % theo goal_type sang mức điều chỉnh
    cố định theo calorie_goal của từng người: maintain=0, cut_light=-300,
    cut_fast=-500, bulk_light=+300, bulk_fast=+500.
    Phê duyệt: owner 2026-08-19 (tùy chỉnh theo nhu cầu cá nhân).
  Templates requiring updates: không.
  Runtime guidance updated: AGENTS.md (đã đồng bộ).
  Follow-up TODOs: không.

  Version change: 2.2.0 → 2.3.0 (2026-08-19)
  Modified principles:
  - §5 Media upload: mở rộng cho BÀI ĐĂNG CỘNG ĐỒNG (video/ảnh), lưu trên
    SeaweedFS self-hosted qua backend proxy (không gọi external API).
    Phê duyệt: owner 2026-08-19 (chọn phương án C, lưu SeaweedFS).
  Version change: 2.3.0 → 2.4.0 (2026-08-27)
  Modified principles:
  - §5 External API allowlist: thêm Tenor embed (GIF bài đăng cộng đồng — chỉ Web
    client, URL do User cung cấp, không gọi từ backend) + Instagram link/preview;
    làm rõ upload media bài đăng cộng đồng: chỉ ảnh PNG/JPG/JPEG/WEBP, KHÔNG GIF/video.
    Phê duyệt: owner 2026-08-27 (clarify Q2/Q3 của 018-fix-social-flows).
  Templates requiring updates: không.
  Runtime guidance updated: AGENTS.md (đã đồng bộ).
  Follow-up TODOs: không.
-->

**Version**: 2.4.0
**Ratified**: 2026-08-17
**Last Amended**: 2026-08-27
**Status**: Active

Constitution này là nguồn quy tắc canonical cho Speckit agents và người review.
`AGENTS.md` và các tài liệu trong `specs/` có thể mở rộng nhưng KHÔNG được mâu thuẫn với file này.

## 1. Scope

WorkoutSmartApp là ứng dụng hỗ trợ người mới tập gym/calisthenics: gợi ý lộ trình tập theo mục tiêu cá nhân (Rule-based, không AI/ML), hướng dẫn kỹ thuật trực quan (GIF/ảnh 180×180), theo dõi tiến trình (sets/reps/weight), kiểm soát thời gian nghỉ và độ tập trung, theo dõi dinh dưỡng (TDEE/calo/macro), xã hội (kết bạn, leaderboard streak) và thống kê báo cáo.

In scope (8 nhóm feature + custom exercise — chi tiết use case trong `specs/General Spec.md` Appendix):

- Core & Auth: UC-01, UC-02, UC-03, UC-20
- Workout Plan: UC-04, UC-05, UC-06
- Workout Tracking: UC-07, UC-08, UC-09
- Hồ sơ & Lịch sử: UC-10, UC-11
- Dinh dưỡng: UC-12, UC-13
- Xã hội: UC-14, UC-15
- Thống kê: UC-17
- Quản trị: UC-18, UC-19

In scope bổ sung:

- User tự tạo bài tập cá nhân (custom exercise) — riêng tư, dùng trong tracking tự do và Rule Engine.
- Upload media cho bài tập tự tạo cá nhân (GIF/ảnh 180×180) — lưu local/S3, không gọi external API.

Out of scope (`General Spec.md` §9):

- Social login (Google/Facebook/Apple) — chỉ Email/Password.
- AI/ML cho gợi ý lộ trình — chỉ Rule-based.
- Live coaching/video call với PT; wearable; chat giữa bạn bè.
- Upload media cho kho bài tập hệ thống — kho hệ thống vẫn dùng `exercises-dataset/` (GIF/ảnh 180×180).
- Thanh toán — đã lược bỏ hoàn toàn khỏi scope.

## 2. Immutable Tech Stack

- Backend: Spring Boot 3.3 + Java 17 + Maven (`mvn` / `./mvnw`) — Web API (REST JSON), KHÔNG dùng server-rendered MVC (Thymeleaf/JSP).
- Web: React 18 + TypeScript (strict) + Vite — SPA gọi REST API; giao diện tuân thủ `DESIGN.md` (theme tối). Không dùng MVC pattern phía web.
- Mobile: Flutter (Dart) + `flutter_lints`; triển khai SAU web; khi code phải thống nhất MỘT giải pháp state management duy nhất (không trộn nhiều giải pháp).
- Database: PostgreSQL 18.
- ORM: Spring Data JPA / Hibernate. Code ứng dụng KHÔNG dùng raw SQL.
- Auth: JWT (access token 15 phút + refresh token 7 ngày) + bcrypt.
- DB migration: Flyway.
- API docs: OpenAPI / Swagger.
- Backend tests: JUnit 5 + Mockito.
- Web tests: Jest + React Testing Library; lint ESLint + Prettier.
- Mobile tests: `flutter test` + `flutter analyze`.
- Thư viện hỗ trợ (Lombok, MapStruct, Jackson, Axios, React Hook Form...) được phép khi không thay thế stack.

## 3. Architecture Principles

Backend phải theo kiến trúc phân lớp: Controller → Service → Repository → Entity.

- Controller: HTTP, DTO, validation, status code; KHÔNG chứa business logic.
- Service: business rules, transaction, authorization, audit log.
- Repository: Spring Data JPA interface.
- Entity: chỉ map bảng database.

Mọi endpoint ghi phải validate DTO bằng Bean Validation (Jakarta Validation). Lỗi xử lý tập trung: 400 validation; 401/403 auth; 404 not found; 409 conflict; 422 vi phạm business rule.

Functional Requirements viết bằng cú pháp EARS (WHEN/WHERE/THE hệ thống SHALL/PHẢI), tiếng Việt.

## 4. Domain Invariants (đã chốt — không tự ý thay đổi)

- **Streak** có định nghĩa DUY NHẤT toàn hệ thống: chuỗi TUẦN liên tiếp đạt ≥ 3 buổi tập; bỏ 1 tuần → reset về 0. Leaderboard và thống kê dùng cùng định nghĩa này.
- **Leaderboard** là kỳ thi vô tận (không reset theo chu kỳ) + Challenge có thời hạn do Admin tạo.
- **Rule Engine v1**: sinh lộ trình từ goal_type × fitness_level × equipment (bảng luật trong `General Spec.md`); KHÔNG AI/ML.
- **TDEE** = BMR Mifflin-St Jeor × hệ số vận động (1.2–1.9); mục tiêu calo theo `calorie_goal` của từng người (tùy chỉnh theo nhu cầu): maintain = giữ nguyên TDEE, cut_light = −300 kcal, cut_fast = −500 kcal, bulk_light = +300 kcal, bulk_fast = +500 kcal. Thiếu thông số (cân nặng/chiều cao/tuổi/giới tính/mức vận động) → yêu cầu nhập đủ trước khi tính.
- **Sync offline**: DUY NHẤT Last-Write-Wins; `workout_sets` UPSERT theo (session_id, session_exercise_id, set_number) — phiên bản cũ không có snapshot vẫn UPSERT (session_id, set_number); client có double-tap guard (chỉ gửi 1 request).
- **Workout session** auto-expire khi sang ngày mới (giờ địa phương): status active/completed/interrupted/expired.
- **Tài khoản**: soft-delete 30 ngày (hủy xóa/khôi phục được; đăng ký lại cùng email → khôi phục nguyên vẹn); ban → middleware chặn mọi request + revoke access & refresh token ngay.
- **Retention**: chi tiết bữa ăn (`meal_entries`) giữ 2 tuần, cũ hơn chỉ giữ tổng kết ngày (`meal_daily_summaries`); thực phẩm custom bị xóa → draft 1 tuần → xóa cứng.
- **Bữa ăn** đánh số Bữa 1..n; mặc định 3 bữa/ngày, tự thêm, hôm sau reset về 3 bữa.
- **Quản lý bài tập**: bài ẩn biến mất khỏi Rule Engine + tìm kiếm nhưng vẫn hiện trong lịch sử; nếu nằm trong lộ trình active → thông báo + clone draft queue + gợi ý bài thay thế (cùng nhóm cơ/chuyển động).
- **KHÔNG có tính năng thanh toán.**

## 5. Audit, Security & Data Integrity

- Mọi thao tác Admin (khóa/mở khóa user, thêm/sửa/ẩn bài tập) phải ghi audit log: actor, action, target type/id, reason, timestamp. Audit log append-only.
- KHÔNG commit secrets/mật khẩu/API key/JWT secret; KHÔNG đọc `.env`, `*.secret`, `credentials/*`.
- External API allowlist: Email Service (SendGrid / AWS SES / SMTP Gmail — gửi OTP), FCM (push — chỉ Mobile), YouTube IFrame Player API + Spotify embed (nhạc luyện tập — chỉ Web client, URL do User cung cấp, không gọi từ backend), Tenor embed + Instagram link/preview (GIF bài đăng cộng đồng — chỉ Web client, URL do User cung cấp, không gọi từ backend). Media hệ thống dùng `exercises-dataset/` local; upload media mới cho bài tập tự tạo cá nhân (lưu local/S3) VÀ cho bài đăng cộng đồng (chỉ ảnh PNG/JPG/JPEG/WEBP — KHÔNG GIF/video — lưu SeaweedFS self-hosted qua backend proxy) — không gọi external API.
- JWT middleware phải check trạng thái ban ở mọi request, kể cả token còn hạn; ban → revoke token ngay, hiệu lực khi user kết nối lại (chặn ≤ 3 giây ở request kế tiếp).
- Mật khẩu lưu bằng bcrypt; không lưu mật khẩu thô.
- Master data soft-delete; dữ liệu giao dịch (session, bữa ăn) không xóa cứng trước thời hạn retention đã chốt.

## 6. Code Quality

- Java: Google Java Style; ưu tiên constructor injection; không `System.out` trong production.
- React: ESLint + Prettier, TypeScript strict, function components + hooks.
- Flutter: `flutter_lints`; widget nhỏ gọn.
- Không để lại TODO trong code hoàn thành; comment giải thích "why", không phải "what".
- Hàm ~40 dòng, file ~300 dòng khi khả thi.
- JPA entity có lazy relationship KHÔNG dùng Lombok `@Data`.

## 7. Migration Lifecycle

- KHÔNG được xóa migration files.
- Migration đã áp dụng ở môi trường dùng chung là immutable — thay đổi schema phải tạo migration mới.
- Không trùng version Flyway trong lịch sử chạy được.
- Cleanup migration KHÔNG được xóa business data.

## 8. Speckit Artifact Gates

Mọi feature spec/plan/tasks phải thể hiện rõ (khi liên quan):

- Use case (UC) và nhóm feature tương ứng.
- Thay đổi entity/bảng + ảnh hưởng Flyway migration.
- DTO validation của request/response.
- Quy tắc authorization (role + trạng thái tài khoản).
- State transitions (session, account, friendship...).
- Test unit cho service/business rule + integration test cho endpoint.
- Cập nhật OpenAPI/Swagger cho mọi endpoint mới/sửa.

## 9. Testing & Definition Of Done

Task chỉ xong khi:

- Test service/business logic pass với coverage ≥ 80% cho phần code thay đổi.
- Integration test endpoint phủ happy path + error path.
- `mvn test` (backend), `npm test`/`npm run build` (web), `flutter test`/`flutter analyze` (mobile) pass.
- HTTP status code đúng chuẩn cho error cases.
- Thao tác Admin có audit log.

## 10. Git & Review (Git Flow)

- Dùng Git Flow (git-flow-next): nhánh `main`, `develop`, `feature/*`, `release/*`, `hotfix/*`, `support/*`.
- KHÔNG commit trực tiếp vào `main`/`develop` — mọi thay đổi qua nhánh và merge bằng `git flow finish`.
- Release chỉ fix bug nhỏ + tài liệu, không thêm tính năng.
- Commit theo Conventional Commits (feat/fix/docs/chore), subject ≤ 50 ký tự, imperative mood.
- PR cần ≥ 1 approval, giữ dưới 400 dòng thay đổi; việc lớn nên tách nhỏ.

## 11. Governance

Constitution này override mọi hướng dẫn mâu thuẫn trong dự án. Nếu file khác mâu thuẫn, sửa file đó hoặc sửa constitution một cách tường minh.

Sửa đổi cần:

1. PR hoặc yêu cầu rõ ràng từ owner nêu lý do + ảnh hưởng.
2. Review các template Speckit và file hướng dẫn phụ thuộc (`AGENTS.md`).
3. Semantic versioning:
   - MAJOR: xóa/định nghĩa lại nguyên tắc không tương thích.
   - MINOR: thêm nguyên tắc mới hoặc mở rộng đáng kể.
   - PATCH: chỉnh wording không đổi ngữ nghĩa.
