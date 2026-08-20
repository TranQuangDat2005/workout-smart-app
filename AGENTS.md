# AGENTS.md — Dự án: WorkoutSmartApp
# Phiên bản: 1.8.0 | Cập nhật: 2026-08-19 | Tác giả: Dattq

## 1. MỤC TIÊU & VAI TRÒ
Bạn là một kỹ sư phần mềm senior trong dự án.
Mục tiêu chính: Xây dựng ứng dụng hỗ trợ người mới tập gym/calisthenics — tự động gợi ý lộ trình tập theo mục tiêu cá nhân (Rule-based), cung cấp hướng dẫn kỹ thuật trực quan (GIF/ảnh 180×180), theo dõi tiến trình tập (sets/reps/weight), kiểm soát thời gian nghỉ & độ tập trung, kèm theo dõi dinh dưỡng (TDEE/calo/macro), tính năng xã hội (kết bạn, leaderboard streak) và thống kê báo cáo.
Stack công nghệ: React 18 (Web App — SPA gọi REST API, tuân thủ DESIGN.md), Flutter (Mobile App — triển khai sau web), Spring Boot 3.3 (Backend — Web API REST, không MVC), PostgreSQL 18. Authentication: JWT (access token 15 phút + refresh token 7 ngày).

## 2. PHẠM VI HOẠT ĐỘNG
### Được phép:
- Đọc và chỉnh sửa: `specs/` (toàn bộ spec theo quy trình speckit), `docs/` (tài liệu, ADR), code trong `backend/`, `web/`, `mobile/` khi các thư mục này được tạo.
- Chạy build/test: `mvn test` / `./mvnw test` (backend), `npm test` / `npm run build` (web), `flutter test` / `flutter analyze` (mobile).
- Sử dụng skills trong `.claude/skills/` (speckit-specify → plan → tasks → implement) đúng workflow.
- Tạo nhánh theo Git Flow (xem §6): `feature/*`, `release/*`, `hotfix/*`, `support/*` — dùng công cụ `git flow` (git-flow-next).

### Cấm tuyệt đối:
- KHÔNG được xóa migration files.
- KHÔNG được commit trực tiếp vào `main` và `develop` — mọi thay đổi phải đi qua nhánh feature/hotfix và merge bằng `git flow finish`.
- KHÔNG được đọc: `.env`, `*.secret`, `credentials/*`.
- KHÔNG được gọi external API ngoài allowlist: Email Service (SendGrid / AWS SES / SMTP Gmail — gửi OTP), Push Notification (FCM — chỉ Mobile), YouTube IFrame Player API + Spotify embed (nhạc luyện tập — chỉ Web client, URL do User cung cấp, không gọi từ backend). Media bài tập hệ thống dùng từ thư mục local `exercises-dataset/`; upload media mới cho bài tập tự tạo cá nhân (custom exercise, lưu local/S3) VÀ cho bài đăng cộng đồng (ảnh, lưu SeaweedFS self-hosted qua backend proxy).
- KHÔNG được tự ý thêm tính năng ngoài scope đã loại trừ trong `specs/General Spec.md` §9 (Social Login, AI/ML, wearable, chat, live coaching, thanh toán). Lưu ý: "User tự tạo bài tập cá nhân" và "upload media cho custom exercise" đã được đưa vào scope (feature 011-custom-exercise).

## 3. QUY TẮC CODE
- **Spec trước code**: mọi thay đổi tính năng phải có spec tương ứng trong `specs/`. Functional Requirements viết theo cú pháp EARS (WHEN/WHERE/THE hệ thống SHALL/PHẢI), tiếng Việt.
- **Java (Spring Boot)**: Google Java Style; controller mỏng, logic nằm ở service layer; validation dùng Bean Validation; API REST với HTTP status chuẩn (400 validation, 401/403 auth) và thông điệp lỗi rõ ràng.
- **Flutter (Dart)**: `flutter_lints`; tách UI/state (thống nhất một giải pháp state management khi bắt đầu code); widget nhỏ gọn, có test.
- **React (TypeScript)**: ESLint + Prettier; TypeScript strict; function components + hooks.
- **Test coverage tối thiểu: 80%** cho mọi module mới (backend: JUnit + Mockito; mobile: flutter test; web: Jest + React Testing Library).
- **Commit message**: Conventional Commits (feat/fix/docs/chore), subject ≤ 50 ký tự, imperative mood.

## 4. XỬ LÝ LỖI
- Nếu không chắc chắn, hỏi thay vì đoán.
- Ghi log chi tiết trước khi thực hiện thay đổi destructive.
- Tạo backup trước khi refactor file > 200 dòng.
- Khi debug: tái hiện lỗi trước khi sửa; sửa root cause, không patch bề mặt; bổ sung test chặn regression.

## 5. NGỮ CẢNH DỰ ÁN
- **Spec gốc**: `specs/General Spec.md` (v0.4) — ĐỌC TRƯỚC khi làm bất cứ việc gì. Chi tiết từng feature nằm ở `specs/NNN-feature-name/spec.md` (8 nhóm: 001-profile-history, 002-nutrition-tracking, 003-social-community, 005-stats-reports, 006-admin-management, 007-core-auth, 008-workout-plan, 009-workout-tracking).
- **Workflow speckit**: `.specify/` + `.claude/skills/speckit-*`. Constitution chính thức: `.specify/memory/constitution.md` (v2.0.0) — nguồn quy tắc canonical, speckit-plan/tasks phải đối chiếu theo đó.
- **Trạng thái hiện tại**: dự án đang ở giai đoạn SPEC (chưa có code backend/web/mobile). Đừng tạo cấu trúc code khi chưa có plan/tasks được duyệt.
- **Quyết định kiến trúc quan trọng đã chốt** (không tự ý thay đổi):
  1. Gợi ý lộ trình: Rule Engine v1 (map theo goal_type × fitness_level × equipment), KHÔNG dùng AI/ML.
  2. Streak có định nghĩa duy nhất toàn hệ thống: chuỗi tuần liên tiếp đạt ≥ 3 buổi tập; leaderboard là kỳ thi vô tận + Challenge có thời hạn.
  3. TDEE = BMR Mifflin-St Jeor × hệ số vận động (1.2–1.9); mục tiêu calo theo `calorie_goal` của từng người (tùy chỉnh): maintain = giữ nguyên TDEE, cut_light = −300 kcal, cut_fast = −500 kcal, bulk_light = +300 kcal, bulk_fast = +500 kcal.
  4. Sync offline conflict: DUY NHẤT Last-Write-Wins; `workout_sets` UPSERT theo (session_id, session_exercise_id, set_number); double-tap guard phía client.
  5. Xóa tài khoản = soft-delete 30 ngày, cho phép hủy xóa/khôi phục; ban user = middleware chặn mọi request + revoke token ngay.
  6. Data retention: chi tiết bữa ăn giữ 2 tuần, cũ hơn chỉ giữ tổng kết ngày; thực phẩm custom xóa → draft 1 tuần rồi xóa cứng.
  7. KHÔNG có tính năng thanh toán (đã lược bỏ khỏi scope).
  8. Session workout auto-expire khi sang ngày mới (giờ địa phương).
- **Tài liệu tham khảo**: `specs/General Spec.md` đóng vai trò tài liệu kiến trúc chính (CLAUDE.md chưa tồn tại); ADR khi có sẽ đặt tại `docs/ADR/`.
- **Sprint hiện tại**: [link to Jira/Notion] — chưa cấu hình, cập nhật khi có.

## 6. QUY TRÌNH GIT FLOW
Dự án dùng mô hình Git Flow (hướng dẫn chi tiết đầy đủ trong `gitflow.md`):
- **Nhánh**: `main` (production, gắn tag version), `develop` (tích hợp mọi thay đổi), `feature/*`, `release/*`, `hotfix/*`, `support/*`.
- **Feature** (tính năng mới): `git flow feature start <tên>` từ develop → code → `git flow feature finish <tên>` (merge về develop).
- **Release** (chuẩn bị phát hành): `git flow release start <version>` từ develop — chỉ fix bug nhỏ + tài liệu, không thêm tính năng → `git flow release finish <version>` (merge vào main + develop, gắn tag version).
- **Hotfix** (sửa lỗi khẩn cấp trên bản đã phát hành): `git flow hotfix start <tên>` từ main → `git flow hotfix finish <tên>` (merge vào main + develop, gắn tag).
- **Support** (duy trì bản phát hành cũ): `git flow support start <version>` từ tag version cũ → `git flow support finish <version>`.
- Commit message trong mọi nhánh vẫn theo Conventional Commits (feat/fix/docs/chore), subject ≤ 50 ký tự, imperative mood.
