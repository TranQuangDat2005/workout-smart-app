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

## 3. Functional Requirements
**Core & Auth & Workout Plan (UC-01 → UC-06, UC-20):**
* THE hệ thống SHALL mã hóa mật khẩu người dùng (bcrypt) và cấp phát JWT token khi đăng nhập thành công.
* WHEN người dùng quên mật khẩu (UC-20), THE hệ thống SHALL gửi OTP qua email để xác thực và cho phép đặt lại mật khẩu mới.
* WHEN người tập hoàn thành bước thiết lập mục tiêu (giảm cân/tăng cơ) và thiết bị sẵn có, THE hệ thống SHALL sử dụng logic Rule-based để tự động tạo một lộ trình (Workout Plan) phân bổ vào các ngày trong tuần. Bảng luật Rule Engine v1: `goal_type` quyết định số ngày/tuần, cấu trúc, số reps và thời gian nghỉ — Giảm cân: 4-5 ngày/tuần, cardio + full-body, 12-15 reps, nghỉ 45-60s; Tăng cơ: 4 ngày/tuần split Push/Pull/Legs, 8-12 reps, nghỉ 60-90s; Sức bền: 3-4 ngày/tuần, circuit toàn thân, 15-20 reps, nghỉ 30-45s. `fitness_level` quyết định khối lượng: beginner 2-3 bài/ngày × 3 sets; intermediate 4-5 bài × 4 sets; advanced 5-6 bài × 4-5 sets. `equipment` lọc tập bài từ kho theo dụng cụ sẵn có.
* THE hệ thống SHALL hiển thị chi tiết bài tập bằng ảnh động (GIF), ảnh tĩnh (180x180 thumbnail) và hướng dẫn từng bước khi người dùng ấn vào một bài tập.

**Workout Tracking & Focus Detection (UC-07 → UC-09):**
* WHEN người tập nhấn nút "Hoàn thành hiệp" (Done set), THE hệ thống SHALL lưu dữ liệu hiệp tập (reps, weight) vào cơ sở dữ liệu và tự động đếm ngược thời gian nghỉ (Rest Timer).
* WHILE đồng hồ đếm ngược thời gian nghỉ đang chạy, THE hệ thống SHALL phát âm thanh/rung cảnh báo ở 5 giây cuối cùng trên thiết bị di động.
* WHERE người tập rời khỏi màn hình bài tập liên tục quá 15 giây trong lúc ĐANG TẬP (không tính thời gian Rest Timer đang chạy):
  - Trên Web (dựa vào `document.visibilityState == 'hidden'`): THE hệ thống SHALL cộng 1 vào `focus_interruptions_count`.
  - Trên Mobile (Flutter - dựa vào `AppLifecycleState.paused/inactive`): THE hệ thống SHALL cộng 1 vào `focus_interruptions_count` và gửi Local Push Notification nhắc nhở: "Đừng phân tâm, quay lại tập nào!".

* WHEN tài khoản bị khóa (banned) bởi Admin trong lúc người tập đang ở giữa buổi tập, THE hệ thống SHALL chặn người dùng ở request kế tiếp (độ trễ tối đa 3 giây), giữ lại dữ liệu đã ghi và đánh dấu session là `interrupted`. Lệnh ban có hiệu lực ngay khi người dùng kết nối lại online (kể cả khi đang offline tại thời điểm bị ban); mọi request đi qua middleware đều kiểm tra trạng thái ban và token của tài khoản bị ban bị vô hiệu hóa ngay.

**Hồ sơ & Lịch sử tập (UC-10, UC-11):**
* THE hệ thống SHALL hiển thị đầy đủ thông tin hồ sơ cá nhân (tên, ảnh đại diện, tuổi, cân nặng, chiều cao, mục tiêu) khi User truy cập trang "Hồ sơ cá nhân". Hồ sơ mặc định ở chế độ **Private** (chỉ bạn bè mới xem được).
* THE hệ thống SHALL cho phép User chỉnh sửa hồ sơ (tên, ảnh, tuổi, chiều cao, mục tiêu) với validation. Cân nặng KHÔNG sửa ở đây mà đồng bộ từ `body_metrics`.
* WHEN User thay đổi mục tiêu (goal_type) trong hồ sơ, THE hệ thống SHALL cảnh báo việc archive plan cũ sẽ làm mất tiến trình tập của ngày hôm nay và đưa ra 2 lựa chọn: (1) tạo plan mới NGAY (plan cũ → `archived`, plan mới `active`) hoặc (2) plan mới bắt đầu từ NGÀY MAI (plan cũ tiếp tục có hiệu lực đến hết hôm nay). Lịch sử tập cũ vẫn liên kết với plan cũ.
* WHEN User yêu cầu xóa tài khoản, THE hệ thống SHALL chuyển trạng thái tài khoản sang `deleted` (soft-delete). Trong 30 ngày User có thể hủy yêu cầu xóa. Soft-delete chỉ thay đổi trạng thái hiển thị — mọi liên kết dữ liệu (lịch sử tập, bạn bè, feed, dinh dưỡng) được giữ nguyên và chỉ bị ẩn khỏi các bảng khác. WHEN User đăng ký lại bằng đúng email trong vòng 30 ngày, THE hệ thống SHALL cho phép khôi phục tài khoản với toàn bộ dữ liệu nguyên vẹn. Sau 30 ngày dữ liệu mới bị hard-delete vĩnh viễn.
* THE hệ thống SHALL hiển thị lịch sử buổi tập theo thứ tự thời gian ngược (ngày, thời lượng, số hiệp, tổng khối lượng nâng). User chọn 1 buổi → hiển thị chi tiết từng bài, từng hiệp (reps, weight). Lịch sử tập hỗ trợ phân trang, mặc định 20 buổi/trang, sắp xếp theo `start_time` giảm dần.

**Dinh dưỡng & Theo dõi Calories (UC-12, UC-13):**
* WHEN User nhấn "Thêm bữa ăn", THE hệ thống SHALL hiển thị giao diện chọn bữa và cho phép thêm NHIỀU món trong một bữa: tìm kiếm thực phẩm, nhập khẩu phần (gram) từng món, tự động tính calo + macro (protein, carb, fat) cho cả bữa. Bữa ăn được đánh số Bữa 1, Bữa 2, ..., Bữa n trong ngày; mặc định mỗi ngày 3 bữa, ngày nào ăn nhiều hơn thì tự thêm bữa, sang ngày hôm sau reset về 3 bữa.
* WHEN User muốn sửa một bữa ăn đã ghi nhận, THE hệ thống SHALL cho phép sửa khẩu phần/thực phẩm và tự động tính lại calo. KHÔNG cho phép xóa bữa ăn.
* THE hệ thống SHALL lưu bữa ăn vào nhật ký dinh dưỡng ngày hôm đó, cập nhật tổng calo tiêu thụ và so sánh với mục tiêu calo (target). TDEE = BMR (Mifflin-St Jeor) × hệ số vận động do User chọn (1.2 / 1.375 / 1.55 / 1.725 / 1.9). Mục tiêu calo mặc định theo goal: cutting = TDEE − 15~20%, bulking = TDEE + 10~15%, hoặc User tùy chỉnh mức thâm hụt/thặng dư. WHERE thiếu thông số tính TDEE (cân nặng, chiều cao, tuổi, giới tính, mức vận động), THE hệ thống SHALL yêu cầu User nhập đủ trước khi hiển thị so sánh.
* WHERE không tìm thấy thực phẩm trong kho chung, THE hệ thống SHALL cho phép User tự định nghĩa thực phẩm mới (tên, calo, protein, carb, fat trên 100g) và lưu vào kho cá nhân; tìm kiếm thực phẩm SHALL hợp nhất kết quả từ kho chung và kho cá nhân. User có thể chỉnh sửa hoặc xóa thực phẩm do chính mình tạo trong kho cá nhân; KHÔNG được sửa/xóa thực phẩm thuộc kho chung. Thực phẩm custom khi bị xóa sẽ soft-delete (`deleted_at`): ẩn khỏi tìm kiếm, không dùng được cho bữa mới nhưng tên vẫn hiển thị trong lịch sử bữa ăn cũ; sau 1 tuần bị xóa cứng.
* Retention dữ liệu bữa ăn: chi tiết từng món được giữ cho tuần hiện tại và tuần trước đó. Các tuần cũ hơn, hệ thống SHALL xóa chi tiết món khỏi database, chỉ giữ bản tổng kết ngày (Bữa 1/2/.../n mỗi bữa tổng bao nhiêu calo). Thực phẩm custom khi bị User xóa: giữ draft để hiển thị trong lịch sử nhưng không dùng được cho bữa mới; sau 1 tuần bị xóa cứng cùng bản ghi.
* WHEN User nhấn "Cập nhật chỉ số", THE hệ thống SHALL cho phép nhập cân nặng (bắt buộc) và các chỉ số tùy chọn (% mỡ, vòng eo, vòng ngực, vòng tay), lưu kèm timestamp, tính chênh lệch so với lần nhập gần nhất → hiển thị xu hướng tăng/giảm.

**Xã hội & Cộng đồng (UC-14, UC-15):**
* WHEN User nhấn "Kết bạn" trên hồ sơ người khác, THE hệ thống SHALL gửi lời mời kết bạn (Max 5 lời mời/ngày/User). Không cho gửi lại khi lời mời đang pending. Nếu gửi lại cho người đã từ chối: cooldown 30 ngày. WHERE hai người gửi lời mời cho nhau gần như đồng thời, THE hệ thống SHALL lấy ai nhấn trước (timestamp sớm hơn) làm người gửi; phía còn lại nút "Kết bạn" tự động chuyển thành "Chấp nhận / Từ chối" lời mời đang chờ.
* WHEN người nhận chấp nhận kết bạn, THE hệ thống SHALL thiết lập quan hệ hai chiều, cả hai thấy hoạt động trên feed (Feed chỉ hiển thị cho bạn bè). Mỗi user tối đa 500 bạn bè. Feed hoạt động chỉ hiển thị sự kiện trong 7 ngày gần nhất.
* Quyền riêng tư hoạt động như Threads/Facebook: hồ sơ private thì người lạ chỉ xem được tên hiển thị và hạng, không xem được bài đăng trên tường cá nhân; bài đăng công khai (public) thì ai cũng xem được.
* WHEN User nhấn "Hủy kết bạn", THE hệ thống SHALL xóa quan hệ bạn bè, ngừng hiển thị trên feed của nhau và cập nhật lại leaderboard nhóm bạn.
* THE hệ thống SHALL tính streak theo định nghĩa DUY NHẤT trong toàn hệ thống: Chuỗi tuần liên tiếp có ≥ 3 buổi tập; bỏ 1 tuần không đạt 3 buổi thì streak reset về 0. Leaderboard chỉ hiển thị Tên hiển thị (display_name) và Hạng (rank). WHEN nhiều user có cùng streak, hệ thống SHALL tie-breaking theo thời gian duy trì sớm hơn (ai đạt streak trước thì xếp trên). Leaderboard cập nhật theo batch mỗi 5 phút thay vì realtime để tối ưu hiệu năng.
* Bảng xếp hạng streak là kỳ thi DÀI VÔ TẬN (không reset theo chu kỳ): ai đang giữ chuỗi tuần dài nhất thì đứng đầu. Ngoài ra, Admin có thể tạo các Thử thách (Challenge) có thời hạn (ví dụ "Thử thách 180 ngày Cutting") để người dùng tham gia; khi thử thách kết thúc, hệ thống SHALL tổng kết, lưu lịch sử xếp hạng của thử thách đó.

**Thống kê & Báo cáo (UC-17):**
* THE hệ thống SHALL tổng hợp và hiển thị: biểu đồ cân nặng theo thời gian (dữ liệu UC-13), biểu đồ volume (tổng kg nâng/tuần), streak (chuỗi tuần liên tiếp đạt ≥ 3 buổi tập), tỷ lệ hoàn thành Workout Plan (%), calo tiêu thụ vs. calo nạp trung bình 7 ngày gần nhất (dữ liệu cũ hơn 2 tuần lấy từ bảng tổng kết ngày). Dữ liệu thống kê được tính toán theo batch (pre-computed) và cache để đảm bảo hiệu năng. Calo tiêu thụ trong buổi tập được ước tính dựa trên loại bài tập + thời lượng using MET (Metabolic Equivalent of Task) formula.
* THE hệ thống SHALL hỗ trợ lọc thống kê theo khoảng thời gian (7 ngày / 30 ngày / 90 ngày / tùy chọn).

**Quản trị hệ thống — Admin (UC-18, UC-19):**
* THE hệ thống SHALL cho phép Admin tìm kiếm người dùng theo email/tên/ID, xem chi tiết hồ sơ & lịch sử tập, khóa/mở khóa tài khoản (kèm lý do). Lệnh khóa có hiệu lực khi người dùng kết nối lại online: middleware kiểm tra trạng thái ban ở mọi request và chặn trong tối đa 3 giây kể từ request kế tiếp; token của tài khoản bị khóa bị vô hiệu hóa ngay. Mọi thao tác ghi vào audit log.
* WHEN Admin thêm/sửa/ẩn bài tập trong thư viện, THE hệ thống SHALL lưu thay đổi, tự động invalidate cache liên quan. Bài tập bị ẩn (inactive) biến mất khỏi Rule Engine (UC-05) và tìm kiếm User nhưng vẫn hiển thị trong lịch sử tập cũ. WHERE bài tập bị ẩn đang nằm trong lộ trình của người dùng, THE hệ thống SHALL thông báo cho người dùng, clone tạm thời bài tập vào draft queue để họ tập nốt buổi hiện tại (clone bị xóa sau khi buổi tập hoàn thành), đồng thời gợi ý bài tập thay thế (cùng nhóm cơ, cùng kiểu chuyển động, dụng cụ giống hoặc khác) nếu có.

**Offline & Sync Management (UC-21):**
* THE hệ thống SHALL yêu cầu kết nối mạng khi đăng nhập. Sau khi xác thực thành công, THE hệ thống SHALL batch fetch toàn bộ dữ liệu user (profile, workout plan hiện tại, meals tuần hiện tại, body_metrics, exercise library) và lưu vào Local Storage (SQLite) trên thiết bị.
* WHERE thiết bị mất kết nối mạng trong lúc người tập đang ghi nhận hiệp tập (sets/reps), THE ứng dụng SHALL lưu dữ liệu vào local queue. ONLY workout tracking data (workout_sessions, workout_sets) được phép offline — tất cả các tính năng khác (meals, body metrics, profile editing, friends, social) SHALL yêu cầu online và hiển thị thông báo lỗi khi không có mạng.
* WHEN thiết bị khôi phục kết nối mạng (chuyển đổi online→offline→online), THE ứng dụng SHALL tự động trigger sync: push toàn bộ local queue (workout_sets chưa sync) lên server theo thứ tự thời gian (chronological order).
* THE ứng dụng SHALL cung cấp Sync Management Screen với các thành phần: (a) Trạng thái hiện tại (● Đang đồng bộ / ● Đã đồng bộ / ● Lỗi), (b) Thời gian đồng bộ gần nhất (timestamp), (c) Số items chờ đồng bộ (pending count), (d) Nút "Đồng bộ ngay" (manual sync trigger).
* WHEN sync conflict xảy ra với workout_sets, THE hệ thống SHALL xử lý Last-Write-Wins dựa trên `client_timestamp`: bản ghi có timestamp mới hơn thắng. Server KHÔNG hiển thị conflict dialog cho user — ghi đè im lặng.
* WHERE offline session sync lên nhưng session đã hết hạn trên server (đã sang ngày mới so với `start_time` theo timezone user), THE server SHALL reject toàn bộ sets thuộc session đó và trả về HTTP 409 Conflict kèm danh sách rejected set IDs.
* WHERE offline session sync lên nhưng `session_id` chưa tồn tại trên server (offline session mới hoàn toàn, chưa từng sync), THE server SHALL tạo mới bản ghi trong `workout_sessions` và `workout_sets` với timestamps giữ nguyên từ client.

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
* **`friendships`:** `id`, `user_id_1`, `user_id_2`, `status` (pending/accepted/rejected), `initiated_by`, `created_at`, `updated_at`.
* **`activity_feed`:** `id`, `user_id`, `action_type` (workout_completed/streak_milestone/new_pr), `details_json`, `created_at`.
* **`leaderboard_entries`:** `id`, `user_id`, `current_streak_weeks`, `longest_streak_weeks`, `rank`, `updated_at` — bảng xếp hạng kỳ thi vô tận (không reset theo chu kỳ).
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
