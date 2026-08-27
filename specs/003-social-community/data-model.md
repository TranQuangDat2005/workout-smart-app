# Data Model: Xã hội & Cộng đồng (003-social-community)

## Bảng (migration V7 — đã tạo)

| Bảng | Mô tả |
|---|---|
| `friendships` | user_id_1, user_id_2, status (pending/accepted/rejected/superseded), initiated_by |
| `activity_feed` | user_id, action_type (friendship_created/streak_milestone/new_pr), details_json |
| `leaderboard_entries` | current_streak_weeks, longest_streak_weeks, streak_start_week, rank |
| `challenges` | name, goal_type, duration_days, start/end_date, status (open/closed/finished) |
| `challenge_participants` | challenge_id, user_id, joined_at, completed_at, final_rank |
| `community_posts` | user_id, content, media_url, media_type, gif_url, audience (public/friends/private) |

## Quy tắc nghiệp vụ

- Lời mời: KHÔNG giới hạn số lượng/ngày (đã gỡ bỏ — supersede 018); pending → 409; rejected trong 30 ngày → 429; lời mời chéo → auto-accept (ai nhấn trước là người gửi) — 018 FR-001; rút lời mời pending → DELETE row — 018 FR-WITHDRAW.
- Hủy kết bạn: friendship chuyển `superseded` (không xóa row — constitution §7).
- Privacy: email chỉ hiển thị cho bạn bè; leaderboard chỉ có display_name + rank; search hiển thị relationship_status cho từng kết quả — 018 FR-004.
- Streak = chuỗi tuần liên tiếp ≥3 buổi completed; tuần hiện tại chưa đủ thì chưa tính đứt — constitution §4.
- Challenge: chỉ Admin tạo; join 1 lần (422 nếu lặp hoặc status ≠ open hoặc đã quá end_date) — 018 FR-012.
- Challenge lifecycle: chỉ lưu status `open` → `finished` (tổng kết xong); trạng thái "qua end_date chờ tổng kết" = `closed` SUY RA khi truy vấn — 018 FR-010/FR-CHALL-CLOSE.
- Challenge tổng kết: streak hiện tại tại thời điểm end_date; tie-break = streak_start_week ASC → userId ASC — 018 FR-011.
- Media bài đăng: chỉ upload PNG/JPG/JPEG/WEBP ≤10MB (kiểm tra magic bytes); GIF dùng embed URL từ tenor.com/instagram.com — 018 FR-014 + FR-015.
- Community post audience: public (mọi người) / friends (bạn bè) / private (chỉ mình); search hiển thị relationship_status cho private — 018 FR-004.
