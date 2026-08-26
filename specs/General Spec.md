# Global Spec
# Version: 0.1 (APPROVED) | Owner: Trần Quang Đạt (@dattq) | Date: 2026-08-17

## 1. Context & Goal
**Business problem:** Người mới bắt đầu tập gym/calisthenics thường gặp khó khăn như: Không biết bắt đầu từ bài tập nào, thiếu kiến thức về kỹ thuật đúng, lo lắng về chấn thương, khó duy trì động lực, không biết cách xây dựng lộ trình phù hợp với mục tiêu. Ngoài ra, nhiều người dễ mất tập trung trong lúc tập (dùng điện thoại, nghỉ quá lâu, không theo dõi thời gian), dẫn đến hiệu quả thấp và dễ bỏ cuộc.

**Feature goal:** Xây dựng ứng dụng hỗ trợ người dùng tập luyện, tự động gợi ý lộ trình dựa trên mục tiêu, cung cấp hướng dẫn kỹ thuật trực quan. Đồng thời theo dõi tiến trình tập (sets/reps/weight), kiểm soát độ tập trung và thời gian nghỉ (rest timer) để duy trì động lực.

**Success metric:**
- 70% người dùng duy trì thói quen tập luyện ít nhất 3 buổi/tuần trong vòng 21 ngày.
- Tăng tỷ lệ người dùng đạt mục tiêu cá nhân lên 80%.
- 60% người dùng báo cáo rằng họ tập trung hơn nhờ tính năng kiểm soát thời gian nghỉ và nhắc nhở phân tâm.

**Tech context:**
Stack: React 18 (Web App), Flutter (Mobile App), Spring Boot 3.3, PostgreSQL 18.
Kiến trúc: Backend Spring Boot = Web API (REST JSON thuần, không server-rendered MVC/Thymeleaf); Web = React SPA gọi REST API; Mobile = Flutter gọi cùng REST API. Thứ tự triển khai: Web trước, Flutter sau.

## 2. Actors & Roles
| Actor | Loại | Mô tả | Permissions |
|-------|------|-------|-------------|
| **Người tập (Trainee)** | Internal | Người dùng cuối đăng nhập qua Email/Password. | Thiết lập mục tiêu, nhận lộ trình, tracking buổi tập (sets/reps), xem hướng dẫn, xem hồ sơ & lịch sử tập, xóa tài khoản, ghi nhận dinh dưỡng, theo dõi chỉ số cơ thể, kết bạn, xem xếp hạng, xem thống kê. |
| **Admin** | Internal | Nhân sự nội bộ (truy cập qua portal riêng). | Quản lý kho 1324+ bài tập (thêm/sửa/ẩn), trigger import data bulk, quản lý tài khoản người dùng (khóa/mở khóa). Mọi thao tác ghi audit log. |
| **Email Service** | External | Dịch vụ gửi email bên ngoài (ví dụ: SendGrid, AWS SES, SMTP Gmail). Backend gọi API để gửi mã OTP xác thực khi đăng ký/đặt lại mật khẩu. | Gửi email OTP theo yêu cầu từ backend; không truy cập trực tiếp vào dữ liệu hệ thống. |
| **Push Notification Service** | External | Dịch vụ gửi push notification (FCM — Firebase Cloud Messaging). Chỉ áp dụng trên Mobile (Flutter). | Gửi remote push notification theo yêu cầu từ backend (ví dụ: nhắc lịch tập, thông báo streak). Không áp dụng cho Web. |

**Actors KHÔNG có trong scope:**
- Guest user (bắt buộc đăng ký/đăng nhập để hệ thống cá nhân hóa lộ trình).
- Personal Trainer (PT).


## 4. Non-functional Requirements
* **Trải nghiệm người dùng (UX):** Giao diện phải được thiết kế tối giản, vận dụng chữ kiệm trong công nghệ thông tin để cho những người không biết về công nghệ thông tin vẫn hiểu và có thể bắt đầu buổi tập với ít hơn 3 lượt chạm (clicks).
* **Hiệu năng:** Thời gian phản hồi API lấy chi tiết bài tập < 300ms (P95).
* **Tính sẵn sàng (Offline Cache):** Ứng dụng Flutter cần cache dữ liệu lộ trình tập (Plan) của tuần hiện tại bằng Local Storage (SQLite/SharedPreferences) để người dùng có thể mở app xem bài tập và chạy timer ngay cả khi không có mạng.

## 5. Data Model
Sử dụng PostgreSQL 18. Các bảng cốt lõi:

### Bảng gốc (UC-01 → UC-09, UC-20)
* **`users`:** `id`, `email`, `password_hash`, `email_verified` (boolean, mặc định false — kích hoạt sau khi xác thực OTP), `role` (user/admin), `goal_type` (weight_loss, muscle_gain, endurance), `fitness_level`, `sex` (male/female), `activity_level` (sedentary/light/moderate/active/very_active), `display_name`, `avatar_url`, `age`, `weight_kg` (snapshot auto-sync từ `body_metrics`), `height_cm`, `account_status` (active/banned/deleted), `created_at`.
* **`exercises`:** `id`, `name`, `category`, `body_part`, `equipment`, `target`, `muscle_group`, `image`, `gif_url`, `instructions` (JSONB đa ngôn ngữ), `status` (active/inactive), `created_at`, `updated_at`.
* **`workout_plans`:** `id`, `user_id`, `name`, `goal_type`, `status` (active/archived).
* **`workout_plan_days`:** `id`, `plan_id`, `day_of_week`.
* **`workout_plan_exercises`:** `id`, `day_id`, `exercise_id`, `target_sets`, `target_reps`.
* **`workout_sessions`:** `id`, `user_id`, `plan_id`, `start_time`, `end_time`, `status` (active/completed/interrupted/expired), `focus_interruptions_count`.
* **`workout_sets`:** `id`, `session_id`, `exercise_id`, `set_number`, `reps_completed`, `weight_used`, `rest_time_seconds`.

### Bảng mới — Dinh dưỡng (UC-12, UC-13)
* **`food_items`:** `id`, `name`, `calories_per_100g`, `protein_per_100g`, `carb_per_100g`, `fat_per_100g`, `source` (system/user_custom), `created_by`, `deleted_at` (nullable — thực phẩm custom bị xóa chỉ ẩn khỏi tìm kiếm, giữ làm draft hiển thị lịch sử, xóa cứng sau 1 tuần).
* **`meal_logs`:** `id`, `user_id`, `meal_number` (Bữa 1..n trong ngày), `log_date`, `created_at` — 1 dòng = 1 bữa; tổng calo/macro được tính từ các món bên dưới.
* **`meal_entries`:** `id`, `meal_log_id`, `food_item_id`, `portion_grams`, `total_calories`, `total_protein`, `total_carb`, `total_fat` — từng món trong một bữa.
* **`meal_daily_summaries`:** `id`, `user_id`, `log_date`, `summary_json` — bản tổng kết ngày (mỗi bữa tổng bao nhiêu calo) thay thế chi tiết đã bị xóa theo chính sách retention.
* **`body_metrics`:** `id`, `user_id`, `weight_kg`, `body_fat_pct`, `waist_cm`, `chest_cm`, `arm_cm`, `recorded_at`.

### Bảng mới — Xã hội (UC-14, UC-15)
* **`friendships`:** `id`, `user_id_1`, `user_id_2`, `status` (pending/accepted/rejected/superseded), `initiated_by`, `created_at`, `updated_at`. Bất biến: tối đa 1 bản ghi HOẠT ĐỘNG (khác superseded) cho mỗi cặp.
* **`activity_feed`:** `id`, `user_id`, `action_type` (friendship_created/streak_milestone/new_pr — giá trị workout_completed giữ trong enum nhưng KHÔNG emit để tránh spam), `details_json`, `created_at`.
* **`leaderboard_entries`:** `id`, `user_id`, `current_streak_weeks`, `longest_streak_weeks`, `streak_start_week`, `rank`, `updated_at` — bảng xếp hạng kỳ thi vô tận (không reset theo chu kỳ).
* **`challenges`:** `id`, `name`, `goal_type`, `duration_days`, `start_date`, `end_date`, `status` (open/closed/finished), `created_by` — thử thách có thời hạn do Admin tạo.
* **`challenge_participants`:** `id`, `challenge_id`, `user_id`, `joined_at`, `completed_at`, `final_rank`.

### Bảng mới — Quản trị (UC-18, UC-19)
* **`audit_logs`:** `id`, `admin_id`, `action_type`, `target_type` (user/exercise), `target_id`, `reason`, `details_json`, `created_at`.

### Bảng mới — Offline & Sync (UC-21)
**Lưu ý:** Bảng `local_workout_sessions` và `local_workout_sets` là schema SQLite LOCAL trên thiết bị, KHÔNG tồn tại trên PostgreSQL server. Chúng chỉ dùng để queue dữ liệu offline trước khi sync lên server.

* **`local_workout_sessions` (Local SQLite):** `local_id` (UUID primary key, client-generated), `server_session_id` (nullable — populated sau khi sync thành công), `plan_id`, `start_time`, `status`, `synced` (boolean, mặc định false).
* **`local_workout_sets` (Local SQLite):** `local_id` (UUID primary key, client-generated), `local_session_id` (FK → local_workout_sessions), `exercise_id`, `set_number`, `reps_completed`, `weight_used`, `rest_time_seconds`, `client_timestamp`, `synced` (boolean, mặc định false).
* **`sync_metadata` (Local SQLite):** `user_id`, `last_sync_at` (timestamp lần sync cuối), `pending_count` (số items chờ sync), `sync_status` (idle/syncing/error).

## 6. Error Handling & Security
* WHERE JWT access token (expiry 15 phút) hết hạn giữa buổi tập, THE client SHALL tự động dùng refresh token (expiry 7 ngày) để lấy access token mới (silent refresh). Nếu refresh token cũng hết hạn, kick về màn hình đăng nhập và lưu dữ liệu vào offline queue.
* WHERE tài khoản bị khóa (banned), THE hệ thống SHALL vô hiệu hóa (revoke) ngay access token và refresh token của tài khoản đó; mọi request của tài khoản bị khóa bị middleware từ chối.
* WHERE kết nối mạng bị gián đoạn trong lúc lưu kết quả Set tập, THE ứng dụng Mobile SHALL lưu dữ liệu vào offline queue. WHEN thiết bị khôi phục kết nối mạng, THE ứng dụng SHALL tự động trigger sync (push queue lên server) — KHÔNG dùng periodic retry.
* WHERE xảy ra conflict khi sync dữ liệu offline (ví dụ đăng nhập trên 2 thiết bị), THE hệ thống SHALL xử lý DUY NHẤT theo chiến lược Last-Write-Wins: bản ghi đến sau (timestamp mới hơn) thắng, không hiển thị cảnh báo conflict. Đối với `workout_sets`, hệ thống thực hiện UPSERT dựa trên `(session_id, session_exercise_id, set_number)`; với các dữ liệu khác (bữa ăn, chỉ số cơ thể), request được server xử lý sau cùng quyết định giá trị lưu.
* WHERE phiên buổi tập còn `active` nhưng đã sang ngày mới (giờ địa phương) so với ngày bắt đầu, THE hệ thống SHALL tự động đánh dấu session là `expired`.
* WHERE người dùng nhấn nhanh nhiều lần một nút lưu (double-tap), THE client SHALL kiểm tra trạng thái "đang gửi" và chỉ gửi đúng 1 request.
* WHERE file GIF của bài tập không thể tải do lỗi mạng hoặc timeout, THE client SHALL tự động fallback hiển thị ảnh tĩnh `image`.
* WHERE file bulk import JSON bị thiếu trường bắt buộc (ví dụ thiếu `name` hoặc `gif_url`), THE backend API SHALL reject file đó, rollback transaction và trả về HTTP 400 kèm dòng lỗi chi tiết.
* WHERE offline workout_sets sync lên server nhưng session đã hết hạn (đã sang ngày mới so với `start_time` theo timezone user), THE server SHALL reject toàn bộ sets thuộc session đó, giữ nguyên dữ liệu trong local queue (không xóa), và trả về HTTP 409 Conflict kèm danh sách rejected set IDs. THE client SHALL hiển thị Notification Screen chi tiết danh sách items bị reject và cung cấp nút "Thử lại" (manual retry) cho user.
* WHERE offline workout_sets sync lên nhưng `session_id` chưa tồn tại trên server (offline session mới hoàn toàn), THE server SHALL tạo mới bản ghi trong `workout_sessions` và `workout_sets` với timestamps giữ nguyên từ client.
* WHERE sync conflict xảy ra trên cùng `(session_id, set_number)` từ 2 thiết bị khác nhau, THE server SHALL xử lý Last-Write-Wins dựa trên `client_timestamp`: bản ghi có timestamp mới hơn ghi đè bản cũ. Server KHÔNG hiển thị conflict dialog — ghi đè im lặng và log vào audit.

## 7. Acceptance Criteria
**Core (UC-01 → UC-09):**
* [ ] Admin có thể import dữ liệu của 1324 bài tập vào DB thành công thông qua API nội bộ và không bị duplicate — duplicate xác định theo cặp (tên đã chuẩn hóa + equipment); import lại cùng file thực hiện upsert và trả báo cáo inserted/updated/skipped.
* [ ] Khi user chọn "Body weight" và "Giảm cân", hệ thống sinh ra một Plan chứa các bài tập thuộc category cardio/waist không cần thiết bị.
* [ ] Nhấn nút "Hoàn thành hiệp" trên app sẽ ghi nhận thành công 1 record vào bảng `workout_sets` và Rest Timer bắt đầu chạy.
* [ ] User thu nhỏ (minimize) ứng dụng Flutter khi đang trong bài tập quá 15 giây sẽ nhận được 1 Local Notification nhắc nhở.
* [ ] User có thể xem lịch sử các buổi tập trước đó (thời gian bắt đầu, kết thúc, số lần phân tâm và danh sách set đã tập).
* [ ] Session bị bỏ quên từ ngày hôm trước tự động chuyển trạng thái expired khi người dùng mở lại app.

**Hồ sơ & Lịch sử tập (UC-10, UC-11):**
* [ ] User có thể xem và chỉnh sửa hồ sơ cá nhân (tên, ảnh, tuổi, chiều cao, mục tiêu). Dữ liệu không hợp lệ bị từ chối kèm lỗi inline.
* [ ] Khi User thay đổi mục tiêu, hệ thống hiển thị dialog hỏi có muốn tạo lại Workout Plan.
* [ ] User có thể yêu cầu xóa tài khoản, hệ thống đưa vào trạng thái soft-delete 30 ngày.
* [ ] User có thể hủy yêu cầu xóa trong 30 ngày; đăng ký lại cùng email trong 30 ngày sẽ khôi phục tài khoản với toàn bộ dữ liệu nguyên vẹn.
* [ ] User có thể xem danh sách lịch sử buổi tập theo thứ tự thời gian ngược và xem chi tiết từng buổi.

**Dinh dưỡng (UC-12, UC-13):**
* [ ] User có thể ghi nhận bữa ăn và sửa lại (nhưng không xóa), hệ thống tính calo/macro tự động.
* [ ] Một bữa ăn có thể chứa nhiều món; mặc định 3 bữa/ngày, người dùng có thể thêm bữa trong ngày.
* [ ] Tổng calo ngày được cập nhật và so sánh với mục tiêu calo (TDEE ± mức cutting/bulking/tùy chỉnh) sau mỗi bữa ăn ghi nhận; khi thiếu thông số cơ thể, hệ thống nhắc nhập đủ trước khi tính TDEE.
* [ ] User có thể nhập chỉ số cơ thể (cân nặng bắt buộc + các chỉ số tùy chọn), dữ liệu cân nặng đồng bộ lên bảng `users`, và hệ thống hiển thị xu hướng tăng/giảm.

**Xã hội (UC-14, UC-15):**
* [ ] User có thể tìm kiếm, gửi lời mời kết bạn (tuân thủ rate limit). Người nhận chấp nhận → quan hệ hai chiều.
* [ ] User có thể hủy kết bạn, feed và leaderboard tự động cập nhật.
* [ ] Bảng xếp hạng streak (kỳ thi vô tận, không reset) hiển thị đúng thứ tự theo chuỗi tuần hiện tại; tên và hạng hiển thị công khai; thử thách (challenge) có thời hạn được tổng kết đúng thời điểm kết thúc.

**Thống kê (UC-17):**
* [ ] User có thể xem dashboard thống kê gồm: biểu đồ cân nặng, biểu đồ volume, streak, tỷ lệ hoàn thành Plan (cả active & archived), calo tiêu thụ vs. nạp.
* [ ] User có thể lọc thống kê theo 7 ngày / 30 ngày / 90 ngày / tùy chọn.

**Quản trị (UC-18, UC-19):**
* [ ] Admin có thể tìm kiếm, khóa/mở khóa tài khoản người dùng. Mọi thao tác ghi audit log.
* [ ] Admin có thể thêm mới bài tập (tên, nhóm cơ, GIF, thumbnail, hướng dẫn) vào thư viện.
* [ ] Admin ẩn bài tập → bài tập không xuất hiện trong Rule Engine và tìm kiếm User, nhưng vẫn hiển thị trong lịch sử tập cũ.
* [ ] Khi Admin ẩn bài tập đang nằm trong lộ trình active, user nhận thông báo kèm gợi ý bài tập thay thế.

## 8. Use Case Relationships (toàn bộ 21 UC)
```
UC-01 ──include──► UC-02    (Đăng ký include OTP verification)
UC-20 ──include──► UC-02    (Quên mật khẩu include OTP verification)
UC-04 ──include──► UC-05    (Thiết lập mục tiêu include tạo Workout Plan)
UC-07 ──include──► UC-08    (Bắt đầu tập include Rest Timer)
UC-10 ──extend───► UC-05    (Đổi mục tiêu trong hồ sơ → hỏi tạo lại plan)
UC-13 ──include──► UC-17    (Dữ liệu chỉ số cơ thể đổ vào báo cáo)
UC-07 ──include──► UC-17    (Dữ liệu buổi tập đổ vào báo cáo)
UC-14 ──extend───► UC-15    (Bạn bè → bảng xếp hạng riêng nhóm bạn)
UC-19 ──extend───► UC-05    (Admin ẩn bài tập → Rule Engine loại trừ)
UC-07 ──include──► UC-21    (Workout tracking offline → sync khi reconnect)
UC-21 ──include──► UC-03    (Sync management cần user đang đăng nhập)
```

## 9. Out of Scope
### KHÔNG thực hiện trong sprint/phase này:
* Social Login (Google, Facebook, Apple) - Chỉ hỗ trợ Email/Password cho Phase 1.
* Thuật toán AI Machine Learning cho việc gợi ý (Tạm thời chỉ dùng Rule-based map theo tags).
* Video call / Live coaching với PT.
* Tích hợp thiết bị wearable (Apple Watch, Fitbit, v.v.).
* Tính năng chat / nhắn tin giữa bạn bè.

### Lý do loại trừ:
* Trọng tâm của version 0.3 là mở rộng từ core flow sang full-stack features: Hồ sơ + Dinh dưỡng + Xã hội + Thống kê + Quản trị. ML, wearable, chat sẽ được xem xét sau khi validate tính năng hiện tại.
* Lưu ý: "User tự tạo bài tập cá nhân" (custom exercise) đã được đưa vào scope từ feature 011-custom-exercise — KHÔNG còn nằm trong Out of Scope.

## 10. Notes / Open Questions
* Hướng dẫn ngôn ngữ (i18n): Tạm thời default hiển thị tiếng Anh (en) từ field `instructions` cho toàn bộ app. Nâng cấp bộ dịch tiếng Việt (vi) sẽ được đưa vào backlog phase tiếp theo.
* Upload Avatar user: Lưu file ở đâu (AWS S3 hay thư mục public của backend)?
* Database thực phẩm: Cần seed từ nguồn dữ liệu nào? (USDA FoodData Central, Nutritionix, hay dataset Việt Nam)?
* Công thức TDEE: ĐÃ CHỐT — BMR Mifflin-St Jeor × hệ số vận động (xem mục 3. Dinh dưỡng).

## Appendix: Bảng tổng hợp 21 Use Cases
| #     | Use Case                                 | Actor chính          | Nhóm tính năng         | Loại EARS      |
|-------|------------------------------------------|----------------------|------------------------|----------------|
| UC-01 | Đăng ký tài khoản                        | User                 | Core & Auth            | WHEN           |
| UC-02 | Xác thực OTP                             | User, Email Service  | Core & Auth            | WHEN           |
| UC-03 | Đăng nhập                                | User                 | Core & Auth            | WHEN           |
| UC-04 | Thiết lập mục tiêu                       | User                 | Workout Plan           | WHEN           |
| UC-05 | Tạo Workout Plan (Rule-based)            | System               | Workout Plan           | State-driven   |
| UC-06 | Xem chi tiết bài tập                     | User                 | Workout Plan           | State-driven   |
| UC-07 | Bắt đầu buổi tập & ghi nhận hiệp        | User                 | Workout Tracking       | WHEN           |
| UC-08 | Rest Timer & cảnh báo                    | System               | Workout Tracking       | WHILE          |
| UC-09 | Phát hiện phân tâm (Focus Detection)     | System               | Workout Tracking       | WHERE          |
| UC-10 | Xem & chỉnh sửa hồ sơ cá nhân          | User                 | Hồ sơ & lịch sử tập   | State-driven   |
| UC-11 | Xem lịch sử buổi tập                    | User                 | Hồ sơ & lịch sử tập   | State-driven   |
| UC-12 | Ghi nhận bữa ăn & tính calories         | User                 | Dinh dưỡng             | WHEN           |
| UC-13 | Theo dõi chỉ số cơ thể theo thời gian   | User                 | Dinh dưỡng             | WHEN           |
| UC-14 | Kết bạn & theo dõi người dùng khác      | User                 | Xã hội                 | WHEN           |
| UC-15 | Xếp hạng thi đấu (Leaderboard)          | System, User         | Xã hội                 | State-driven   |
| UC-17 | Xem thống kê & báo cáo tiến độ          | User                 | Thống kê               | State-driven   |
| UC-18 | Quản lý người dùng (Admin)              | Admin                | Quản trị               | State-driven   |
| UC-19 | Quản lý nội dung bài tập (Admin)        | Admin                | Quản trị               | WHEN           |
| UC-20 | Quên mật khẩu / Reset Password          | User, Email Service  | Core & Auth            | WHEN           |
| UC-21 | Offline Tracking & Sync Management       | User, System         | Workout Tracking       | WHERE          |
