# Brainstorm Report — Feed cộng đồng kiểu mạng xã hội (018-social-feed)

**Ngày**: 2026-08-19 | **Tác giả**: AI Agent (brainstorm skill) | **Trạng thái**: Chờ owner duyệt
**Input**: "phần cộng đồng tôi muốn 1 phần nữa như kiểu mạng xã hội sử dụng stream để lướt vô hạn xem bài viết, video ngắn, đăng bài được" + "tự trả lời sao cho giống với facebook" + "có SeaweedFS để lưu media, người dùng lưu ảnh trực tiếp trên server của tôi".

---

## 1. Problem-First

### 1.1 Chẩn đoán solution-jumping
Đề xuất "mạng xã hội + stream lướt vô hạn + video ngắn" là một giải pháp nén. Vấn đề bên dưới nhiều khả năng là: (a) feed hoạt động hiện tại (FR-005) chỉ có sự kiện tự sinh, "khô", thiếu nội dung người dùng tạo; (b) thiếu kênh để người tập chia sẻ tiến trình/hỏi kinh nghiệm → giảm động lực & retention; (c) muốn cảm giác cộng đồng "thật" như Facebook.

### 1.2 Vấn đề thật
Người dùng mới tập cần **kênh nội dung do chính cộng đồng tạo** để chia sẻ, học hỏi và được công nhận — điều mà feed hoạt động tự động không cung cấp được.

### 1.3 Ba cách định hình (đã chọn)
| Frame | Không gian giải pháp | Kết luận |
|---|---|---|
| A. Diễn đàn chia sẻ | Post text + ảnh, comment/like, feed bạn bè | Được giữ lại làm nền |
| B. Content discovery | Feed khám phá công khai, video ngắn embed | Được giữ lại (tab Khám phá) |
| C. Mini social network | Post + video + like/comment/share + infinite scroll | **Đây là lựa chọn cuối** (owner: "giống Facebook") |

### 1.4 Evidence status
Weak — ý tưởng từ owner, chưa có dữ liệu người dùng. → Giảm rủi ro bằng cách phân pha, giới hạn rõ scope v1, giữ các tính năng nặng ở backlog.

---

## 2. Yêu cầu đã chốt (Discovery Phase — owner ủy quyền "giống Facebook")

| # | Chủ đề | Quyết định |
|---|---|---|
| Q1 | Loại nội dung | Text + ảnh upload + video upload (SeaweedFS) + video embed URL (YouTube — hợp lệ allowlist, Web client) |
| Q2 | "Stream" | Infinite scroll bằng cursor pagination REST; realtime (SSE) để backlog |
| Q3 | Tương tác | Like + Comment + Share (repost kèm caption tùy chọn) |
| Q4 | Visibility | Kiểu Facebook: mỗi bài đăng chọn audience **public / friends / private**; 2 tab feed: "Bạn bè" + "Khám phá" |
| Q5 | Moderation | Report bài + Admin ẩn/xóa bài (audit log, liên kết 006-admin-management) |
| Q6 | Vị trí spec | Feature mới `018-social-feed` mở rộng `003-social-community`; **giữ nguyên** feed hoạt động FR-005 (2 feed khác nhau: activity feed tự sinh vs social feed UGC) |
| Q7 | Kiến trúc media | **Phương án A — Backend proxy**: client upload multipart → Spring Boot validate (auth, MIME, kích thước, quota) → PUT lên SeaweedFS (S3 API) → lưu URL vào DB |

---

## 3. Nghiên cứu SeaweedFS (kết luận)

SeaweedFS (Apache-2.0, self-hosted, 1 binary `weed`) **phù hợp hoàn toàn** cho media bài đăng:

- S3-compatible API (`weed s3`, port 8333): `PutObject`, `GetObject`, `PostObject` (form policy), multipart upload, **Presigned URL**, bucket policy (IAM-style), CORS, audit log, quota.
- `GetObject` hỗ trợ **Range requests** → HTML5 `<video>` stream được.
- Volume server hỗ trợ **resize ảnh on-the-fly** (`?height=200&width=200&mode=fit`) — dùng cho thumbnail feed.
- Filer hỗ trợ **TTL** → media của bài bị xóa tự expire (khớp triết lý retention của dự án: soft-delete rồi xóa cứng).
- Filer metadata store dùng được PostgreSQL; triển khai dev bằng `weed mini` (1 lệnh, Docker sẵn có).
- Backend kết nối qua AWS SDK v2 / Spring Cloud AWS với endpoint tùy chỉnh (không gọi external API — SeaweedFS là hạ tầng nội bộ).

### Rào cản pháp lý nội bộ (phải xử lý tường minh)
Constitution §5 hiện ghi *"upload media mới CHỈ cho bài tập tự tạo cá nhân (lưu local/S3)"*. → Cần **sửa Constitution v2.2.0 → v2.3.0 (MINOR)**: mở rộng phạm vi upload media cho **bài đăng cộng đồng**, lưu trên SeaweedFS (self-hosted). Được xử lý trong bước speckit-clarify với phê duyệt của owner.

---

## 4. Thiết kế đề xuất (final)

### 4.1 Phạm vi v1 (spec 018-social-feed)

**P1 — Bài đăng & feed:**
- Đăng bài: text (bắt buộc ≥ 1 ký tự nếu không có media), tối đa 10 ảnh/bài (≤ 10MB/ảnh), tối đa 1 video/bài (≤ 100MB, ≤ 3 phút — "video ngắn"), hoặc 1 URL YouTube embed (không gọi backend — IFrame Player API phía Web client).
- Audience selector mỗi bài: **public / friends / private** (private = chỉ mình xem).
- Sửa/xóa bài của chính mình; xóa = soft-delete, media TTL 30 ngày trên SeaweedFS.
- Feed "Bạn bè": bài public + friends của bạn bè + activity feed FR-005 (giữ nguyên), sắp xếp mới nhất trước.
- Feed "Khám phá": bài public toàn server, mới nhất trước.
- Cả 2 tab: **cursor pagination** (`?cursor=&limit=20`), pull-to-refresh.
- Tường cá nhân (profile): danh sách bài của user, tôn trọng visibility theo quan hệ người xem (đồng bộ FR-012: hồ sơ private → người lạ chỉ thấy display_name + rank).

**P2 — Tương tác:**
- Like (1 like/user/bài, bỏ like được), hiển thị like_count.
- Comment: text ≤ 1000 ký tự, xóa comment của mình, xóa toàn bộ comment khi bài bị xóa; comment **không phải chat** (chat vẫn out of scope — ranh giới: comment gắn với bài đăng công khai, không có nhắn tin 1-1).
- Share: tạo bài mới có `shared_post_id` (kèm caption tùy chọn), tăng share_count bài gốc; bài gốc bị xóa → bài share hiển thị "nội dung không còn tồn tại".

**P3 — Moderation & quản trị:**
- Report bài (reason bắt buộc, 1 report/user/bài), lưu `post_reports`.
- Admin: ẩn bài (ẩn khỏi mọi feed, chủ bài thấy nhãn "bị ẩn"), khôi phục, xóa vĩnh viễn — mọi thao tác ghi audit log (constitution §5).
- Rate limit chống spam: tối đa 10 bài/ngày, 50 comment/ngày, 200 like/ngày (mẫu số tham chiếu FR-002: 5 lời mời/ngày; con số chính xác chốt ở clarify).

**Backlog (KHÔNG làm v1):** SSE realtime, notification (like/comment/share), hashtag, lưu bài, edit comment, đa reaction, story, video transcoding, presigned URL upload.

### 4.2 Dự kiến entity (chi tiết hóa ở spec/data-model)

- `posts` (id, user_id FK, content, visibility enum, shared_post_id FK nullable, like_count, share_count, status active/hidden/deleted, created_at, updated_at, deleted_at)
- `post_media` (id, post_id FK, media_type image/video, media_url — SeaweedFS path, sort_order)
- `post_embeds` (id, post_id FK, platform youtube, embed_url) — hoặc cột trên posts nếu chỉ 1 embed/bài
- `comments` (id, post_id FK, user_id FK, content, created_at, deleted_at)
- `post_likes` (id, post_id FK, user_id FK, created_at; UNIQUE(post_id, user_id))
- `post_reports` (id, post_id FK, reporter_id FK, reason, status pending/resolved, created_at)
- Audit log dùng hạ tầng hiện có của 006.

### 4.3 Ảnh hưởng & ràng buộc

- **Constitution**: sửa §1 (scope thêm "Feed cộng đồng: bài đăng/bình luận/like/share") + §5 (media upload mở rộng cho bài đăng, lưu SeaweedFS) — MINOR bump 2.3.0, qua speckit-clarify.
- **Web**: thêm trang Feed (`/feed`, 2 tab) + trang chi tiết bài (`/posts/:id`) + tường cá nhân mở rộng + nav "Cộng đồng"; upload qua multipart (React + Axios), YouTube embed bằng IFrame API.
- **Mobile (Flutter)**: triển khai sau web (theo constitution); video embed mở link ngoài thay vì iframe nội tuyến.
- **Backend**: module mới `com.workoutsmart.feed` (Controller → Service → Repository → Entity), S3 client cho SeaweedFS; OpenAPI cập nhật; test coverage ≥ 80%.
- **DB**: migration Flyway mới (KHÔNG sửa/xóa migration cũ).

### 4.4 Rủi ro

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| UGC không kiểm duyệt (nội dung xấu) | Cao | Report + admin ẩn + audit log trong v1 (P3) |
| Video upload nặng băng thông qua backend | Trung bình | Giới hạn 100MB/3 phút; presigned URL là lối nâng cấp sẵn có |
| Feature creep (biến thành Facebook thật) | Cao | Phân pha P1→P3, backlog tường minh, YAGNI |
| SeaweedFS vận hành thêm 1 service | Thấp | `weed mini`/Docker; S3 API chuẩn nên dễ thay thế |
| Comment bị hiểu nhầm là "chat" (out of scope) | Thấp | Ghi rõ ranh giới trong spec |

### 4.5 Success metrics

- SC-001: Feed tải trang đầu ≤ 500ms, mỗi trang 20 bài qua cursor pagination.
- SC-002: User đăng bài thành công trong ≤ 30 giây (viết → chọn media → đăng).
- SC-003: 100% report được admin xử lý (ẩn/giữ + audit log) trong 24 giờ.
- SC-004: Bài bị ẩn/xóa biến mất khỏi mọi feed trong ≤ 5 phút (cache invalidation).
- SC-005: Media bài xóa mềm tự expire khỏi SeaweedFS sau 30 ngày (TTL).

---

## 5. Next steps

1. Owner duyệt báo cáo này (đặc biệt: Q1 = B+C video upload, Q7 = backend proxy, phân pha P1→P3).
2. Chạy `/speckit-specify` với input feature → tạo `specs/018-social-feed/spec.md` (EARS, tiếng Việt).
3. Chạy `grill-me` review spec (đã có trong pipeline người dùng yêu cầu).
4. `/speckit-analyze` → `/speckit-clarify` (tại đây chốt sửa Constitution v2.3.0 + các con số rate limit) → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`.
5. Nhánh Git Flow: `feature/018-social-feed` từ `develop`.
