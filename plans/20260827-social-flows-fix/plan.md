---
title: "Fix Social & Community flows (003-social-community)"
description: "Sửa các luồng mạng xã hội (kết bạn, feed hoạt động, leaderboard streak, challenge, privacy/media) cho khớp spec 003 + constitution. 5/6 câu hỏi grill-me đã được owner chốt 2026-08-27 (Q6 chờ confirm, dùng assumption)."
status: pending
priority: P1
branch: "feature/fix-003-social-flows"
tags: [social, friendship, leaderboard, challenge, feed, privacy, spec-alignment]
blockedBy: []
blocks: []
created: "2026-08-26T19:47:15.567Z"
createdBy: "ck:plan"
source: skill
---

# Fix Social & Community flows (003-social-community)

## Overview

Tính năng Xã hội & Cộng đồng (spec `003-social-community`, package `com.workoutsmart.social` + `com.workoutsmart.feed`) đã được implement (tasks.md T001–T010 checked) nhưng **lệch spec ở nhiều điểm nghiêm trọng**: quan hệ bạn bè có thể sinh bản ghi trùng (không có unique constraint), feed hoạt động gần như không có sự kiện nào được phát ra (chỉ `friendship_created`), leaderboard tie-break sai luật và quét toàn bộ DB mỗi lần xem, Challenge không có lifecycle tổng kết (FR-015/SC-003 chưa implement), privacy hồ sơ (FR-012) chưa có field dữ liệu, và chính sách media mâu thuẫn giữa constitution/spec/code.

Plan này sửa toàn bộ luồng đó theo đúng workflow speckit (spec trước code) + grill-me (chốt 6 quyết định owner trước khi code).

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Spec & Contract Alignment](./phase-01-spec-contract-alignment.md) | Pending |
| 2 | [Friendship Integrity & UX](./phase-02-friendship-integrity-ux.md) | Pending |
| 3 | [Activity Feed Events & UI](./phase-03-activity-feed-events-ui.md) | Pending |
| 4 | [Leaderboard Correctness & Performance](./phase-04-leaderboard-correctness-performance.md) | Pending |
| 5 | [Challenge Lifecycle](./phase-05-challenge-lifecycle.md) | Pending |
| 6 | [Privacy & Media Policy](./phase-06-privacy-media-policy.md) | Pending |
| 7 | [Verification & Quality Gates](./phase-07-verification-quality-gates.md) | Pending |

## Findings (evidence base — đã verify trên code hiện tại)

| ID | Severity | Vấn đề | Bằng chứng |
|----|----------|--------|------------|
| C1 | CRITICAL | `friendships` không có unique constraint (user_id_1, user_id_2) → lời mời chéo đồng thời tạo 2 row, gửi lại sau khi bị từ chối tạo row mới; `findBetween` ném ngoại lệ khi >1 row → 500 hàng loạt (search/feed/isFriend) | `V7__social.sql`, `SocialService.sendRequest`, `FriendshipRepository.findBetween` |
| C2 | CRITICAL | Feed hoạt động (FR-005) gần như trống: chỉ `friendship_created` được ghi; `streak_milestone`/`new_pr` (General Spec §5) không bao giờ được phát ra; không lọc 7 ngày; web không có UI nào gọi `/feed` | `SocialService.addFeed`, `ProfileService.completeSession` → **Q5=B: chỉ emit streak_milestone + new_pr** |
| C3 | CRITICAL | Challenge không có lifecycle: không scheduler tổng kết khi đến end_date (FR-015, SC-003), `final_rank`/`completed_at` không bao giờ được tính; status chỉ có "open" | `SocialService.createChallenge/joinChallenge`, `V7__social.sql` |
| H1 | HIGH | Lời mời chéo: code auto-accept, còn spec FR-011/AS7 yêu cầu nút "Chấp nhận/Từ chối". data-model.md + openapi ghi auto-accept → mâu thuẫn nội bộ spec | `spec.md FR-011` vs `data-model.md` vs `SocialService` L115-123 → **Q1: giữ auto-accept, sửa spec.md cho khớp** |
| H2 | HIGH | Tie-break leaderboard (FR-010): spec "thời gian duy trì sớm hơn"; code sort theo `longestStreakWeeks` rồi `userId` | `SocialService.computeLeaderboard` L269-272 → **Q3=A: thêm `streak_start_week`, sort theo streakStartWeek asc** |
| H3 | HIGH | FR-009 (ghim vị trí cá nhân): `GET /leaderboard` không nhận auth, cắt top-100 → user rank >100 không được ghim | `SocialController.leaderboard()` L78-81 |
| H4 | HIGH | FR-012 privacy: `users` không có field `is_private` → không thể enforce "người lạ chỉ xem display_name + rank" | `User.java` (không có field), grep |
| H5 | HIGH | Media policy mâu thuẫn: constitution 2.3.0 (video/ảnh) vs §5 body/AGENTS.md/FR-012b (chỉ ảnh, KHÔNG GIF/video); code cho phép GIF upload; web có code render video (dead code) | `SeaweedStorageService` L28, `CommunityFeedPage` L88-92/L238-240 → **Q2: ảnh upload (bỏ GIF) + GIF embed Tenor/Instagram; sửa constitution §5 + AGENTS.md** |
| H6 | HIGH | Leaderboard tính realtime trên mọi request: `findAll` users + 1000 sessions/user + upsert từng entry → không batch 5 phút như Assumption, rủi ro hiệu năng/DoS | `computeLeaderboard` L246-286 |
| H7 | MEDIUM | Streak tính theo `ZoneId.systemDefault()` (múi giờ server) cho mọi user, vi phạm "giờ địa phương"; chỉ xét 1000 session gần nhất | `StreakCalculator`, `SocialService` L44 |
| M1 | MEDIUM | Search luôn hiện nút "+ Kết bạn" bất kể trạng thái quan hệ (đã là bạn/pending gửi/pending nhận) — AS7 yêu cầu nút theo trạng thái; DTO không có trạng thái | `FriendsPage.tsx` L109, `UserSearchResponse` |
| M2 | MEDIUM | `accept()` không kiểm tra status pending; không có danh sách "đã gửi"; không enforce cap 500 bạn (Assumption) | `SocialService.accept` |
| M3 | MEDIUM | Test gap: `SocialServiceTest` chỉ có 1 test (tasks.md khai 18); FriendsPage/LeaderboardPage không có test; openapi 003 thiếu `/leaderboard/friends` + toàn bộ `/api/v1/feed` | đọc trực tiếp các file test |
| M4 | LOW | Status challenge trôi từ vựng: spec.md `upcoming/active/completed` vs General Spec/data-model `open/closed/finished` vs code chỉ `open` | 3 artifacts → **Q4=B: chuẩn hóa `open/closed/finished`, sửa spec.md** |

## Decision Log (grill-me — owner đã chốt 2026-08-27)

| # | Câu hỏi | Quyết định | Hệ quả cập nhật spec/code |
|---|---------|-----------|---------------------------|
| Q1 | Lời mời chéo | **Auto-accept** (giữ code/data-model/openapi) | Sửa `spec.md` FR-011 + AS7 cho khớp auto-accept (code KHÔNG đổi hành vi này) |
| Q2 | Media bài đăng | **Ảnh upload trực tiếp (KHÔNG GIF/video) + GIF qua embed hệ thống ngoài (Tenor/Instagram)** | Bỏ `gif` khỏi upload; thêm `gif_url` embed (URL do User cung cấp, chỉ Web client, backend không gọi external — cùng pattern YouTube/Spotify); sửa constitution §5 + AGENTS.md (governance PR riêng) |
| Q3 | Tie-break FR-010 | **A — ai đạt streak hiện tại sớm hơn xếp trên** | Thêm `streak_start_week` vào `leaderboard_entries` (migration mới); sort `currentStreak desc, streakStartWeek asc` |
| Q4 | Từ vựng status Challenge | **B — `open/closed/finished`** (General Spec/data-model) | Sửa `spec.md` FR-013/015 về bộ từ này; code map: open = đang mở join, closed = quá end_date chưa tổng kết, finished = đã tổng kết |
| Q5 | Sự kiện feed | **B — chỉ `streak_milestone` + `new_pr`** (không phát `workout_completed` để tránh spam) | Sửa FR-005; `workout_completed` giữ trong enum General Spec nhưng KHÔNG emit (ghi chú trong spec 003) |
| Q6 | Hồ sơ private (FR-012) | ⚠️ CHƯA TRẢ LỜI — dùng assumption **A + C**: field `is_private` do 001-profile-history sở hữu, mặc định **private** (General Spec §3 đã ghi "Hồ sơ mặc định ở chế độ Private") | Chờ confirm; nếu sai thì chỉ đổi spec sở hữu/default, code Phase 6 không đổi cấu trúc |

**Out of Scope (đã chốt — thêm vào spec 003 §Out of Scope):**
- **Block user**: Edge Case nhắc "block/huỷ kết bạn" nhưng không có FR — KHÔNG làm, chỉ hủy kết bạn.
- **Realtime feed** (WebSocket/FCM push) — KHÔNG làm; feed dùng poll + batch.
- **Upload video/GIF lên SeaweedFS** — KHÔNG làm; GIF chỉ qua embed Tenor/Instagram (Web client).

## Dependencies

- Related (không chặn): `plans/20260826-domain-ownership-spec-clarification` (umbrella PM) — plan này là bản execution chi tiết cho domain "Social & Admin" (Person 4).
- Constitution `.specify/memory/constitution.md` v2.3.0 là nguồn canonical; mọi sửa spec phải đối chiếu §4/§5.
- Phase 2–6 phụ thuộc quyết định tương ứng ở Phase 1 (Q1–Q6). Phase 7 chạy song song + gate cuối.

## Git & Workflow

- Nhánh: `git flow feature start fix-003-social-flows` (từ `develop`; KHÔNG commit thẳng `main` — constitution §10). Nếu `develop` chưa tồn tại cục bộ, tạo từ `origin/main`.
- Mọi thay đổi đi kèm spec update (speckit: spec trước code), commit theo Conventional Commits, subject ≤ 50 ký tự.
- Migration mới (V20+) — KHÔNG sửa V7/V19 (constitution §7).
