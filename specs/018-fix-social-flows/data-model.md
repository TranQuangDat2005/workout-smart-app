# Data Model: 018-fix-social-flows

Migration mới V20–V23 (additive — KHÔNG sửa V7/V19, constitution §7).

## V20 — friendships: dedupe + bất biến 1 bản ghi hoạt động/pair

```sql
-- 1. Thêm giá trị trạng thái mới (dữ liệu cũ trùng lặp được giữ, không xóa)
-- status: pending / accepted / rejected / superseded

-- 2. Dedupe: với mỗi cặp (LEAST, GREATEST) có >1 bản ghi hoạt động,
--    giữ bản ghi thắng (accepted > pending > rejected; updated_at mới nhất),
--    các bản ghi còn lại → 'superseded'.

-- 3. Unique index một phần — chỉ 1 bản ghi hoạt động cho mỗi cặp:
CREATE UNIQUE INDEX uq_friendships_active_pair
  ON friendships (LEAST(user_id_1, user_id_2), GREATEST(user_id_1, user_id_2))
  WHERE status <> 'superseded';
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
-- NULL = bài không có GIF. Host allowlist: tenor.com, *.tenor.com, instagram.com, *.instagram.com (validate ở service).
```

## Quy tắc nghiệp vụ (tóm tắt theo spec 018)

- **Friendship**: 1 bản ghi hoạt động/pair (unique index một phần); lời mời chéo → auto-accept; gửi lại sau reject 30 ngày → UPDATE row cũ; cap 5 lời mời/ngày (giữ nguyên); cap 500 bạn → 422.
- **Search**: kèm `relationshipStatus` (none/pending_sent/pending_received/accepted) — 1 query batch.
- **Activity feed**: action_type ∈ {friendship_created, streak_milestone, new_pr}; chỉ bạn bè + 7 ngày + top 50; milestone dedupe theo giá trị streak; PR dedupe theo tổng volume (kg).
- **Leaderboard**: sort `current_streak_weeks DESC, streak_start_week ASC, user_id ASC`; chỉ user ACTIVE; viewer luôn được trả kèm (pin); batch ≤5 phút.
- **Challenge**: status `open → closed → finished` (1 chiều); join chỉ khi `open` và chưa qua end_date; tổng kết ≤1 phút sau end_date; final_rank = streak tại end_date, tie-break như leaderboard.
- **Privacy**: target private → người lạ chỉ thấy display_name + rank; bài `public` ai cũng xem; bạn bè xem bình thường.
- **Media**: upload chỉ PNG/JPG/JPEG/WEBP ≤10MB (magic bytes); gif_url chỉ tenor.com/instagram.com (https); backend không gọi external.
