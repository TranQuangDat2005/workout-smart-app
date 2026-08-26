---
phase: 7
title: "Verification & Quality Gates"
status: pending
priority: P1
dependencies: [2, 3, 4, 5, 6]
---

# Phase 7: Verification & Quality Gates

## Overview
Đóng khoảng trống test (M3) và chạy toàn bộ gate chất lượng (constitution §8/§9): unit + integration backend, test web, build/lint, speckit-analyze consistency, cập nhật tasks.md + openapi cho khớp thực tế.

## Requirements
- Coverage ≥ 80% cho phần code sửa (constitution §9); integration test happy path + error path.
- tasks.md của 003 phản ánh đúng hiện trạng (đang khai 18 test SocialServiceTest nhưng thực tế 1 — phải sửa số liệu sau khi bổ sung test).
- `mvn test` (backend), `npm test` + `npm run build` + lint (web) phải pass.
- Cập nhật openapi.yaml 003: endpoint mới (results, leaderboard/friends đã có trong code, feed 018).

## Architecture
Không đổi kiến trúc; chỉ bổ sung test + đồng bộ artifact.

## Related Code Files
- Create: `backend/src/test/java/com/workoutsmart/social/service/...` (bổ sung test cases còn thiếu)
- Create: `web/src/pages/social/FriendsPage.test.tsx`, `LeaderboardPage.test.tsx`
- Modify: `specs/003-social-community/tasks.md`, `contracts/openapi.yaml`, `data-model.md`
- Modify: `web/src/services/socialApi.ts` (nếu API đổi)

## Implementation Steps
1. Backend unit test bổ sung cho SocialService: sendRequest (rate limit, pending, cooldown, reuse row, cap 500, self-friend), accept/reject guard, feed 7-day filter, leaderboard tie-break (Q3=A: streakStartWeek) + pin, challenge finalizer idempotent (open→closed→finished).
2. Integration test: cross-invite đồng thời → 1 row; hoàn thành buổi → streak_milestone/new_pr event (không có workout_completed); leaderboard pin ngoài top-100; challenge auto-finalize; upload GIF/video rejected; `gif_url` ngoài allowlist rejected (theo Q2).
3. Web test: FriendsPage nút theo trạng thái; LeaderboardPage pin + challenge results (open/closed/finished); CommunityFeedPage media policy + GIF embed.
4. Chạy `mvn test`, `npm test`, `npm run build`, lint; sửa hết lỗi.
5. Chạy speckit-analyze cho 003 (spec/plan/tasks consistency) → không còn CRITICAL.
6. Cập nhật tasks.md (check thật, không khai khống), data-model.md, openapi.
7. Gate cuối: re-read toàn bộ plan + spec (whole-plan consistency sweep) — không còn thuật ngữ cũ (upcoming/active/completed, GIF upload, workout_completed emit).
8. `git flow feature finish fix-003-social-flows` sau khi test + review pass.

## Success Criteria
- [ ] `mvn test` pass; coverage social + feed ≥ 80% (jacoco report).
- [ ] `npm test` + `npm run build` + ESLint pass.
- [ ] tasks.md số liệu khớp thực tế; openapi đủ endpoint.
- [ ] speckit-analyze 003: 0 CRITICAL/HIGH chưa xử lý.
- [ ] Không còn mâu thuẫn giữa spec/code/data-model (sweep pass).

## Risk Assessment
- Số lượng test cần bổ sung lớn → chia theo phase, mỗi phase tự đóng test của mình trước khi sang phase sau (shift-left), Phase 7 chỉ tổng hợp + gate.
- Môi trường build (JAVA_HOME/tsc) từng bị chặn (plan 20260826 ghi nhận) → xác nhận toolchain trước; nếu không build được, báo cáo rõ lệnh lỗi thay vì khai pass.
