---
phase: 5
title: "Challenge Lifecycle"
status: pending
priority: P1
dependencies: [1]
---

# Phase 5: Challenge Lifecycle

## Overview
Implement vòng đời Challenge đầy đủ (C3) theo **Q4=B**: từ vựng `open/closed/finished` (khớp General Spec/data-model). Scheduler tự động tổng kết khi đến `end_date` (FR-015, SC-003), tính `final_rank`/`completed_at` cho participant, chặn join khi hết hạn, expose kết quả cho user + admin.

## Requirements
- Functional: WHEN Challenge đến `end_date`, hệ thống PHẢI chuyển status sang `closed` và tổng kết: tính `completed_at` + `final_rank` cho từng participant, lưu lịch sử xếp hạng, sau đó chuyển `finished`.
- WHEN Challenge có status khác `open` (closed/finished) HOẶC `end_date` đã qua, User PHẢI bị chặn join (422).
- Admin tạo Challenge: validate `durationDays ≥ 1`, `startDate` không trong quá khứ (hoặc default hôm nay); status khởi tạo `open`.
- Xếp hạng Challenge: số tuần đạt ≥3 buổi completed (định nghĩa streak DUY NHẤT — constitution §4) trong khoảng `start_date → end_date`; tie-break như leaderboard (Q3=A: ai đạt sớm hơn xếp trên).
- Non-functional: scheduler chạy mỗi phút, idempotent (chỉ tổng kết 1 lần — guard bằng status + transaction).

## Architecture
- Status machine (Q4=B): `open` (từ lúc tạo đến end_date — join được) → `closed` (end_date đã qua, chờ/đang tổng kết) → `finished` (đã tính final_rank xong). Status chỉ chuyển 1 chiều, update bằng scheduler.
- `@Scheduled(fixedDelay = 60s)` `ChallengeFinalizer`: tìm challenge có `status='open'` và `endDate <= today` → đổi `closed` → với mỗi participant tính số tuần đạt chuẩn trong khoảng challenge → set `completedAt`, `finalRank` (sort + tie-break Q3) → đổi `finished`. Guard: `UPDATE ... WHERE status='closed'` để tránh chạy 2 lần.
- API thêm: `GET /challenges/{id}/results` (danh sách participant + final_rank, chỉ hiện khi `finished`); `ChallengeResponse` thêm `completedAt`/`finalRank` (nếu có).
- Admin UI (web/pages/admin) thêm màn tạo challenge (hiện chưa có UI tạo dù backend có endpoint admin).

## Related Code Files
- Create: `backend/.../social/service/ChallengeFinalizer.java`
- Modify: `backend/.../social/service/SocialService.java` (createChallenge, joinChallenge, challenges, myChallenges, thêm results)
- Modify: `backend/.../social/dto/ChallengeResponse.java`, `CreateChallengeRequest.java` (+ Bean Validation)
- Modify: `backend/.../social/entity/Challenge.java`, `ChallengeParticipant.java`
- Modify: `backend/.../social/repository/ChallengeRepository.java`, `ChallengeParticipantRepository.java`
- Modify: `web/src/pages/social/LeaderboardPage.tsx` (hiển thị results + status đúng), `web/src/pages/admin/*` (form tạo challenge)
- Modify: `specs/003-social-community/spec.md` (FR-013/014/015 theo Q4=B — đã có ở Phase 1)

## Implementation Steps
1. `challenges()` trả danh sách `open`; `myChallenges()` trả theo participant kèm status + final_rank.
2. Sửa `createChallenge`: Bean Validation trên DTO + check `startDate` quá khứ (400) + status `open`.
3. Sửa `joinChallenge`: chặn khi status != `open` hoặc end_date đã qua (422).
4. `ChallengeFinalizer`: tính streak per participant trong khoảng challenge; set completedAt/finalRank; chuyển status `closed` → `finished`; idempotent.
5. Thêm endpoint results + DTO mở rộng.
6. Web: hiển thị "Xếp hạng chung cuộc" cho challenge `finished`; admin form tạo challenge.
7. Test: unit finalizer (idempotent, tie-break Q3), integration join hết hạn 422, results.

## Success Criteria
- [ ] 100% Challenge được tổng kết tự động khi đến end_date (SC-003) — integration test với end_date quá khứ.
- [ ] `final_rank` đầy đủ cho mọi participant sau tổng kết; chạy lại job không đổi kết quả (idempotent).
- [ ] Join challenge closed/finished bị 422; open join được.
- [ ] Admin có UI tạo challenge; user xem được kết quả chung cuộc.
- [ ] `mvn test` pass; coverage ≥ 80%.

## Risk Assessment
- Streak "trong khoảng challenge" chưa được định nghĩa chi tiết trong spec → định nghĩa trong Phase 1 (spec FR-015): số tuần (thứ 2 → CN) nằm trong [start_date, end_date] đạt ≥3 buổi completed.
- Scheduler trong nhiều instance → trùng lặp finalize; v1 chấp nhận 1 instance (ghi chú), guard idempotent bằng status check trong transaction.
- Challenge có thể kết thúc lúc nửa đêm — job 60s đảm bảo tổng kết trong ≤1 phút sau end_date.
