---
phase: 3
title: "Activity Feed Events & UI"
status: pending
priority: P2
dependencies: [1]
---

# Phase 3: Activity Feed Events & UI

## Overview
Hiện feed bạn bè chỉ có sự kiện `friendship_created` (C2). Theo **Q5=B**, phase này phát `streak_milestone` (khi chuỗi tuần tăng + các mốc đẹp) và `new_pr` (kỷ lục volume buổi tập) — KHÔNG phát `workout_completed` để tránh spam — lọc 7 ngày, và thêm UI feed hoạt động trên web (hiện chưa có trang nào gọi `/feed`).

## Requirements
- Functional (theo Q5=B): WHEN chuỗi tuần streak của User tăng lên giá trị mới, hệ thống PHẢI ghi `streak_milestone` (tối đa 1 event/tuần tăng; thêm 1 event khi chạm mốc 10/30/50/100). WHEN User đạt kỷ lục cá nhân về tổng volume (kg) của một buổi tập, hệ thống PHẢI ghi `new_pr`.
- WHEN hai sự kiện cùng (user, action_type) xảy ra lặp (sync offline gọi lại), hệ thống PHẢI dedupe — mỗi buổi chỉ sinh tối đa 1 event mỗi loại.
- Feed chỉ trả sự kiện của bạn bè trong 7 ngày gần nhất (Assumption), sắp xếp giảm dần, cap 50.
- Non-functional: ghi feed lỗi KHÔNG được làm hỏng transaction `completeSession` (log + tiếp tục).

## Architecture
- Publishing tường minh (KISS, không thêm queue/Spring Events): trong transaction `completeSession` của `ProfileService`, gọi `ActivityFeedService.publishMilestone(userId, streak)` sau khi tính streak mới bằng `StreakCalculator` (định nghĩa DUY NHẤT); `publishPr(userId, sessionTotalVolume)` khi volume buổi > max cũ.
- PR v1: tổng volume buổi = Σ(set.reps × set.weight) của session; lưu kỷ lục vào `detailsJson` của event gần nhất (hoặc cột riêng nếu đơn giản hơn — quyết định khi implement, ưu tiên query 1 event gần nhất).
- Tránh vòng phụ thuộc: `ActivityFeedService` đặt trong `com.workoutsmart.social` (profile → social OK).
- API: giữ `GET /api/v1/feed`; thêm filter `since = now - 7d` (`findTop50ByUserIdInAndCreatedAtAfterOrderByCreatedAtDesc`).
- Web: panel "Hoạt động bạn bè" trên HomePage/UserDashboard gọi `socialApi.feed()`, render theo `actionType` (icon/text tiếng Việt), polling 60s (SC-004).

## Related Code Files
- Create: `backend/.../social/service/ActivityFeedService.java`
- Modify: `backend/.../profile/service/ProfileService.java` (completeSession → publish milestone/PR)
- Modify: `backend/.../social/repository/ActivityFeedRepository.java`, `SocialService.feed`
- Modify: `web/src/pages/HomePage.tsx` (hoặc UserDashboard), `web/src/services/socialApi.ts`
- Modify: `specs/003-social-community/spec.md` (FR-005 theo Q5=B — đã có ở Phase 1)

## Implementation Steps
1. `ActivityFeedService.publishStreakMilestone(userId, currentStreakWeeks)` — idempotent: chỉ emit khi giá trị streak mới > giá trị trong event `streak_milestone` gần nhất của user.
2. `ActivityFeedService.publishPr(userId, sessionId, totalVolumeKg)` — emit khi volume > kỷ lục ghi trong `detailsJson` event `new_pr` gần nhất.
3. `ProfileService.completeSession`: sau khi save `completed` → tính streak mới → publish milestone (nếu tăng); tính volume session → publish PR (nếu kỷ lục). Wrap try/catch log warn.
4. Repository: thêm query 7 ngày; `SocialService.feed` dùng query mới.
5. Web: panel feed hoạt động (danh sách "Bạn X đạt streak N tuần" / "phá kỷ lục volume Y kg"), polling 60s.
6. Test: unit idempotent milestone/PR + 7-day filter; integration: hoàn thành buổi → bạn thấy event; web test panel render.

## Success Criteria
- [ ] Hoàn thành buổi thứ 3 trong tuần → bạn bè thấy `streak_milestone` ≤ 1 phút (SC-004).
- [ ] Phá kỷ lục volume → bạn bè thấy `new_pr`; không phát `workout_completed`.
- [ ] Không có event trùng khi sync gọi lại; không có event > 7 ngày trong response.
- [ ] Web hiển thị panel feed hoạt động; `npm test` + build pass.

## Risk Assessment
- Q5=B chọn chống spam → milestone có thể thưa (1/tuần) — chấp nhận, ghi chú trong spec.
- PR theo tổng volume có thể quá nhạy với user mới (mỗi buổi là kỷ lục) → chấp nhận v1 (động lực), có thể thêm ngưỡng sau.
- Ghi feed lỗi làm hỏng transaction tập → try/catch + log warn, không rollback buổi tập.
