# Quickstart: 018-fix-social-flows

Hướng dẫn chạy & kiểm chứng end-to-end cho feature sửa luồng xã hội. Chi tiết test tự động nằm ở `tasks.md`; file này chỉ các kịch bản validate chính.

## Prerequisites

- Backend: JDK 17, Maven, PostgreSQL 18 (docker-compose ở repo root), SeaweedFS filer tại `http://localhost:8888` (config `app.seaweedfs-filer-url`).
- Web: Node 18+, `npm install` trong `web/`.
- Migration chạy tự động qua Flyway khi backend khởi động (V20–V23 mới).

## Kịch bản validate

### 1. Kết bạn — bất biến 1 quan hệ/pair (FR-001, FR-002, FR-004)

```bash
# Gửi lời mời chéo: tạo 2 user, gửi gần như đồng thời
curl -X POST :8080/api/v1/friendships -H "Authorization: Bearer $TOKEN_A" -d '{"targetUserId": 2}'
curl -X POST :8080/api/v1/friendships -H "Authorization: Bearer $TOKEN_B" -d '{"targetUserId": 1}'
# Kỳ vọng: 1 trong 2 trả 201 (auto-accept), bên còn lại 409 "Đã là bạn bè"; DB chỉ 1 row hoạt động
psql -c "SELECT count(*) FROM friendships WHERE status <> 'superseded' AND LEAST(user_id_1,user_id_2)=1 AND GREATEST(user_id_1,user_id_2)=2;"
# → 1
```

```bash
# Tìm kiếm kèm trạng thái quan hệ
curl ":8080/api/v1/users/search?q=..." -H "Authorization: Bearer $TOKEN_A"
# → relationshipStatus đúng: none/pending_sent/pending_received/accepted
```

### 2. Feed thành tích (FR-005, FR-006)

```bash
# User A hoàn thành buổi tập thứ 3 trong tuần (streak tăng) → bạn bè thấy streak_milestone
curl -X POST :8080/api/v1/workout-sessions/{id}/complete -H "Authorization: Bearer $TOKEN_A"
curl ":8080/api/v1/feed" -H "Authorization: Bearer $TOKEN_FRIEND"
# → có item actionType=streak_milestone trong ≤1 phút; không có item cho từng buổi tập thường
```

### 3. Leaderboard (FR-007, FR-008)

```bash
curl ":8080/api/v1/leaderboard" -H "Authorization: Bearer $TOKEN_RANK_101"
# → 2 user cùng streak: người đạt chuỗi sớm hơn xếp trên; row của viewer luôn có mặt (ghim) dù rank > 100
```

### 4. Challenge vòng đời (FR-010, FR-011, FR-012)

```bash
# Admin tạo challenge kết thúc hôm nay; 2 user tham gia
curl -X POST :8080/api/v1/challenges -H "Authorization: Bearer $TOKEN_ADMIN" -d '{"name":"Sprint 1 ngày","durationDays":1}'
curl -X POST :8080/api/v1/challenges/{id}/join -H "Authorization: Bearer $TOKEN_A"
# Đợi scheduler (≤1 phút sau end_date) → status finished, mỗi participant có final_rank
curl ":8080/api/v1/challenges/{id}/results" -H "Authorization: Bearer $TOKEN_A"
```

### 5. Privacy + media (FR-013, FR-014, FR-015)

```bash
# Upload GIF/video đổi đuôi .jpg → 400
curl -X POST :8080/api/v1/feed/posts -H "Authorization: Bearer $TOKEN_A" -F "media=@fake.gif;filename=a.jpg"
# → 400 "Chỉ hỗ trợ ảnh (PNG/JPG/JPEG/WEBP)"

# gif_url ngoài allowlist → 400; tenor.com → 201
curl -X POST :8080/api/v1/feed/posts -H "Authorization: Bearer $TOKEN_A" -F "gifUrl=https://evil.com/a.gif"
# → 400
curl -X POST :8080/api/v1/feed/posts -H "Authorization: Bearer $TOKEN_A" -F "gifUrl=https://tenor.com/view/example-123"
# → 201; web hiển thị iframe Tenor
```

### 6. Automated tests

```bash
cd backend && mvn test          # JUnit + Mockito + MockMvc — phải pass
cd web && npm test && npm run build && npm run lint
```

## Expected outcomes

- Không còn 500 do bản ghi friendship trùng (search/feed/kết bạn).
- Feed có sự kiện milestone/PR thật, không spam, chỉ 7 ngày.
- Leaderboard tie-break đúng luật, luôn ghim vị trí cá nhân, đọc nhanh (không quét toàn bộ session).
- Challenge tự tổng kết đúng hạn với final_rank đầy đủ.
- Media chỉ ảnh; GIF qua Tenor embed / Instagram link+preview.
