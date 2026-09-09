# Data Model: 018-fix-social-flows

Migration mới V20–V24 (additive — KHÔNG sửa V7/V19, constitution §7).

## V20 — friendships: dedupe + bất biến 1 bản ghi hoạt động/pair

```sql
-- Thêm cột: pair_min/pair_max (cặp chuẩn hóa), active_marker (1 = hoạt động, NULL = superseded)
-- Dedupe: với mỗi cặp giữ bản ghi thắng (accepted > pending > rejected; updated_at mới nhất, rồi id nhỏ hơn);
--         bản ghi thua → status 'superseded' (KHÔNG DELETE — constitution §7).
-- Unique index thường (portable H2 + PostgreSQL — đã kiểm chứng H2 không hỗ trợ partial index):
CREATE UNIQUE INDEX uq_friendships_active_pair
  ON friendships (pair_min, pair_max, active_marker);
-- Nhiều bản ghi superseded cùng pair OK (active_marker NULL, NULLs distinct); chỉ 1 bản ghi active/pair.
```

### State transitions (friendship)

```text
(không tồn tại) ──gửi lời mời──► pending ──nhận chấp nhận──► accepted
      ▲                            │                            │
      │                            └──nhận từ chối──► rejected ──┘
      │                                                  │
      └──gửi lại sau 30 ngày (UPDATE cùng row)───────────┘
pending (chéo, 2 bên gửi đồng thời) ──► accepted (auto-accept, FR-001)
accepted ──hủy kết bạn──► (DELETE row — quan hệ kết thúc hoàn toàn)
bất kỳ bản ghi thua khi dedupe ──► superseded (chỉ đọc, không dùng nghiệp vụ)
```

## V21 — leaderboard_entries: streak_start_week

```sql
ALTER TABLE leaderboard_entries ADD COLUMN streak_start_week DATE;
-- NULL = chưa có chuỗi hiện tại (streak 0). Backfill cho entry có current_streak_weeks > 0.
```

`streak_start_week` = thứ 2 của tuần đầu tiên trong chuỗi tuần hiện tại (để tie-break FR-007). Cập nhật cùng lúc với `current_streak_weeks`/`longest_streak_weeks`/`rank`/`updated_at` (FR-009).

## V22 — users: is_private (field do 001-profile-history sở hữu)

```sql
ALTER TABLE users ADD COLUMN is_private BOOLEAN NOT NULL DEFAULT TRUE;
```

Default `true` (private — Clarifications A, General Spec §3). Profile domain (001) quản lý đọc/ghi; social chỉ consume.

## V23 — community_posts: gif_url

```sql
ALTER TABLE community_posts ADD COLUMN gif_url VARCHAR(500);
-- NULL = bài không có GIF. Host allowlist: tenor.com, media.tenor.com, instagram.com, *.instagram.com (validate ở service).
```

## V24 — challenge_participants: unique index (chống double-join race)

```sql
CREATE UNIQUE INDEX uq_challenge_participants_challenge_user
  ON challenge_participants (challenge_id, user_id);
-- FR-011: (challenge_id, user_id) duy nhất — chặn duplicate participant khi double-join race
-- (T5/D2). Migration additive — KHÔNG sửa migration cũ (constitution §7).
```

## Quy tắc nghiệp vụ (tóm tắt theo spec 018)

- **Friendship**: 1 bản ghi hoạt động/pair (unique index một phần); lời mời chéo → auto-accept; gửi lại sau reject 30 ngày → UPDATE row cũ; KHÔNG có giới hạn số lượng lời mời/ngày (đã gỡ bỏ — supersede spec 003); rút lời mời pending → DELETE row (FR-WITHDRAW); cap 500 bạn → 422.
- **Search**: kèm `relationshipStatus` (none/pending_sent/pending_received/accepted) — 1 query batch; CHỈ hiển thị user ACTIVE (FR-VISIBILITY).
- **Activity feed**: action_type ∈ {friendship_created, streak_milestone, new_pr}; chỉ bạn bè + 7 ngày + top 50; milestone dedupe theo giá trị streak; PR dedupe theo tổng volume (kg); loại tác giả BANNED/DELETED (FR-VISIBILITY).
- **Leaderboard**: sort `current_streak_weeks DESC, streak_start_week ASC, user_id ASC`; chỉ user ACTIVE; viewer luôn được trả kèm (pin); user chưa có entry → unranked cuối bảng (FR-008); batch ≤5 phút.
- **Challenge**: status lưu trữ `open → finished` (1 chiều); `closed` là trạng thái SUY RA khi `open && end_date < today` (FR-CHALL-CLOSE); join chỉ khi lưu trữ `open` và chưa qua end_date; duplicate participant bị chặn bởi unique index; tổng kết ≤1 phút sau end_date + idempotent + loại participant BANNED/DELETED; final_rank = streak tại end_date, tie-break như leaderboard.
- **Privacy**: target private → người lạ chỉ thấy display_name + rank; bài `public` ai cũng xem; bạn bè xem bình thường.
- **Audience check**: mọi thao tác like/comment xem comment đều kiểm tra quyền xem bài đăng (FR-AUDIENCE).
- **Media**: upload chỉ PNG/JPG/JPEG/WEBP ≤10MB (magic bytes, suy extension khi thiếu); gif_url chỉ tenor.com (gồm media.tenor.com)/instagram.com (https) và ≤500 ký tự; bài không mang đồng thời media + gif_url; backend không gọi external.
