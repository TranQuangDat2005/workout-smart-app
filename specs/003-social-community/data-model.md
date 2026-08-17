# Data Model: Xã hội & Cộng đồng (003-social-community)

## Bảng (migration V7 — đã tạo)

| Bảng | Mô tả |
|---|---|
| `friendships` | user_id_1, user_id_2, status (pending/accepted/rejected), initiated_by |
| `activity_feed` | user_id, action_type, details_json |
| `leaderboard_entries` | current_streak_weeks, longest_streak_weeks, rank |
| `challenges` | name, goal_type, duration_days, start/end_date, status (open/closed/finished) |
| `challenge_participants` | challenge_id, user_id, joined_at, final_rank |

## Quy tắc nghiệp vụ

- Lời mời: max 5/ngày; pending → 409; rejected trong 30 ngày → 429; lời mời chéo → auto accept (ai nhấn trước là người gửi).
- Hủy kết bạn: xóa friendship (accepted).
- Privacy: email chỉ hiển thị cho bạn bè; leaderboard chỉ có display_name + rank.
- Streak = chuỗi tuần liên tiếp ≥3 buổi completed; tuần hiện tại chưa đủ thì chưa tính đứt.
- Challenge: chỉ Admin tạo (POST /challenges hasRole ADMIN); join 1 lần (422 nếu lặp).
