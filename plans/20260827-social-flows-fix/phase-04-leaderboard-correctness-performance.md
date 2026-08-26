---
phase: 4
title: "Leaderboard Correctness & Performance"
status: pending
priority: P1
dependencies: [1]
---

# Phase 4: Leaderboard Correctness & Performance

## Overview
Sửa tie-break sai luật (H2), ghim vị trí cá nhân (H3/FR-009), persist `rank` + `updated_at` (FR-008), và thay quét realtime toàn bộ DB (H6) bằng batch job 5 phút đúng Assumption + cache.

## Requirements
- Functional: WHEN streak của User thay đổi, hệ thống PHẢI cập nhật `current_streak_weeks`, `longest_streak_weeks`, `rank`, `updated_at` trong `leaderboard_entries` (FR-008).
- Tie-break theo Q3=A (đã chốt): ai đạt streak hiện tại sớm hơn xếp trên — cần thêm `streak_start_week` vào `leaderboard_entries`; sort `currentStreakWeeks desc, streakStartWeek asc`.
- WHERE User xem bảng xếp hạng, vị trí cá nhân PHẢI luôn được ghim (FR-009) kể cả rank > top-100 → `GET /leaderboard` nhận `Authentication`, luôn bổ sung row của viewer vào cuối response (kèm `rank` thật).
- Non-functional: thời gian response < 200ms nhờ batch + cache; leaderboard chỉ hiện display_name + rank (privacy — đã đúng, giữ nguyên).
- Múi giờ streak: dùng zone theo user (H7) — `User` có `timezone`/dùng default khi null; tối thiểu: ghi rõ trong spec, implement khi có field (backlog nhỏ nếu chưa có).

## Architecture
- Batch job: `@Scheduled(fixedDelay = 5min)` `LeaderboardSyncJob` tính lại streak cho các user có session mới (query users có session completed từ lần chạy trước — tránh full scan) rồi cập nhật entries + rank. `GET /leaderboard` đọc thuần từ bảng `leaderboard_entries` (có index theo rank hoặc sort in-memory limit 100 + viewer row).
- Đơn giản hóa v1 (KISS): bỏ `findAll` + per-user 1000-session query trong read path; tính streak từ `WorkoutSessionRepository` qua query tổng hợp (count completed theo user, tuần) hoặc incremental update khi `completeSession` xảy ra (update entry của chính user đó ngay lập tức — SC-002 "≤5 phút" thỏa). Chọn: **incremental on completeSession + scheduled full recompute hằng ngày** (cân bằng độ chính xác/perf).
- `LeaderboardEntry` thêm `streakStartWeek` (Q3=A đã chốt) và `rank` được set khi recompute.

## Related Code Files
- Create: `backend/.../social/service/LeaderboardSyncService.java` (scheduled)
- Modify: `backend/.../social/service/SocialService.java` (computeLeaderboard → read-only + viewer pin; leaderboard() nhận userId)
- Modify: `backend/.../social/controller/SocialController.java` (`GET /leaderboard` + `GET /leaderboard/friends` truyền auth)
- Modify: `backend/.../social/entity/LeaderboardEntry.java`, `LeaderboardRepository.java` (query top + findByUser)
- Modify: `web/src/pages/social/LeaderboardPage.tsx` (pin hoạt động với row viewer được server trả thêm)
- Create: migration `V21__leaderboard_streak_start.sql`

## Implementation Steps
1. Cập nhật entity + migration `streak_start_week` (Q3=A) và index hỗ trợ đọc top 100.
2. `completeSession` → cập nhật streak entry của user ngay (incremental, gồm `streakStartWeek` = tuần đầu của chuỗi hiện tại); `LeaderboardSyncService` chạy mỗi 5 phút recompute các user có thay đổi + rank toàn bộ ACTIVE.
3. `GET /leaderboard`: đọc entries đã tính (top 100) + luôn append row viewer (tính on-the-fly nếu chưa có); `friendsLeaderboard` tương tự với danh sách bạn bè.
4. Tie-break theo Q3=A khi sort: `currentStreakWeeks desc` → `streakStartWeek asc` → `userId asc`; persist `rank`.
5. Web: xử lý response có `myRow` ngoài top-100 (pin cuối bảng).
6. Test: unit sort/tie-break, pin ngoài top-100, incremental update; integration endpoint.

## Success Criteria
- [ ] Tie-break đúng Q3=A (`streakStartWeek asc`); `rank` được persist (FR-008).
- [ ] Viewer rank > 100 vẫn thấy "Vị trí của bạn" (FR-009).
- [ ] Read path không gọi `findAll` users; P99 < 200ms với 10k users (manual check).
- [ ] Leaderboard cập nhật sau hoàn thành buổi tập ≤ 5 phút (SC-002).
- [ ] `mvn test` pass; coverage phần sửa ≥ 80%.

## Risk Assessment
- Incremental + scheduled có thể lệch rank tạm thời (≤5 phút) → chấp nhận vì đúng Assumption batch; ghi chú trong spec.
- `rank` có thể stale giữa 2 lần chạy job → read path không tin cậy `rank` lưu, chỉ dùng khi sort có tie (hoặc bỏ dùng — quyết định khi implement, ưu tiên sort on-the-fly theo streak + tie-break).
- Zone per-user chưa có field → fallback systemDefault + TODO trong spec (đã liệt kê H7).
