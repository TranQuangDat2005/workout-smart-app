---
description: "Task list template for feature implementation"
---

# Tasks: Sửa luồng Xã hội & Cộng đồng (018-fix-social-flows)

**Input**: Design documents từ `specs/018-fix-social-flows/` (spec.md, plan.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md)

**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: BẮT BUỘC — constitution §9 (coverage ≥ 80% phần code đổi + integration test happy/error path).

**Organization**: Nhóm theo user story để mỗi story implement/test/đóng gói độc lập.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Chạy song song được (khác file, không phụ thuộc task chưa xong)
- **[Story]**: US1..US6 theo spec.md
- Mọi description có đường dẫn file chính xác

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Tiền đề pháp lý/process + baseline xanh

- [x] T001 Kiểm tra toolchain (JDK 17 + Maven + Node) và chạy baseline `mvn test` (backend) + `npm test` (web) trên branch `feature/fix-social-flows` — xác nhận XANH trước khi sửa (tham khảo `RUN_COMMANDS.txt`)
- [x] T002 Amendment constitution v2.4.0: thêm "Tenor embed (GIF bài đăng cộng đồng — URL do User cung cấp, chỉ Web client) + Instagram link/preview" vào §5 allowlist + đồng bộ `AGENTS.md` — file `.specify/memory/constitution.md`, `AGENTS.md` (PR riêng theo Governance §11)
- [x] T003 [P] Đồng bộ `specs/General Spec.md`: từ vựng challenge `open/closed/finished`, ghi chú `workout_completed` KHÔNG emit — file `specs/General Spec.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Migration + entity — CHẶN mọi user story

**⚠️ CRITICAL**: Không user story nào bắt đầu trước khi phase này xong (entity là nền của tất cả)

- [x] T004 Tạo migration `backend/src/main/resources/db/migration/V20__friendship_unique.sql` — dedupe (bản ghi thắng accepted > pending > rejected mới nhất; bản ghi thua → `superseded`, KHÔNG DELETE) + unique index một phần `LEAST/GREATEST ... WHERE status <> 'superseded'`
- [x] T005 [P] Tạo migration `backend/src/main/resources/db/migration/V21__leaderboard_streak_start.sql` — thêm cột `streak_start_week DATE`
- [x] T006 [P] Tạo migration `backend/src/main/resources/db/migration/V22__user_is_private.sql` — `users.is_private BOOLEAN NOT NULL DEFAULT TRUE`
- [x] T007 [P] Tạo migration `backend/src/main/resources/db/migration/V23__community_post_gif_url.sql` — `community_posts.gif_url VARCHAR(500)`
- [x] T008 Cập nhật entities khớp migration: `Friendship.java` (status superseded), `LeaderboardEntry.java` (+streakStartWeek), `User.java` (+isPrivate), `CommunityPost.java` (+gifUrl) — `backend/src/main/java/com/workoutsmart/{social,auth,feed}/entity/`

**Checkpoint**: `mvn test` vẫn xanh; DB migrate V20–V23 sạch trên bản sao có dữ liệu trùng

---

## Phase 3: User Story 1 - Kết bạn tin cậy, không lỗi và không trùng lặp (Priority: P1) 🎯 MVP

**Goal**: Bất biến 1 quan hệ hoạt động/pair, không 500, tái dùng bản ghi sau cooldown, cap 500 bạn, nút tìm kiếm đúng trạng thái (FR-001..FR-004)

**Independent Test**: 2 user gửi lời mời ngược chiều gần như đồng thời → đúng 1 row, không lỗi; gửi lại sau 30 ngày bị từ chối → vẫn 1 row/pair; tìm kiếm trả relationshipStatus đúng

### Tests for User Story 1 (viết TRƯỚC — phải FAIL trước khi implement)

- [x] T009 [P] [US1] Mở rộng unit test `backend/src/test/java/com/workoutsmart/social/service/SocialServiceTest.java` — cases: rate limit 5/ngày, pending 409, cooldown 30 ngày 429, tái dùng row rejected, cap 500 bạn 422, tự kết bạn 422, auto-accept lời mời chéo
- [x] T010 [P] [US1] Integration test lời mời chéo đồng thời → 1 row + search trả relationshipStatus — `backend/src/test/java/com/workoutsmart/social/controller/SocialControllerIntegrationTest.java`

### Implementation for User Story 1

- [x] T011 [US1] Sửa `FriendshipRepository.java` (backend/src/main/java/com/workoutsmart/social/repository/): `findBetween` → `List<Friendship>` (không ném khi nhiều row), thêm query batch trạng thái quan hệ cho search (R12) + `countAcceptedFor`
- [x] T012 [US1] Sửa `SocialService.sendRequest` (backend/.../social/service/SocialService.java): reuse row `rejected` khi hết cooldown (UPDATE, không INSERT), guard cap 500, giữ auto-accept (depends T011)
- [x] T013 [US1] Sửa `SocialService.searchUsers` + `UserSearchResponse.java` (backend/.../social/): thêm `relationshipStatus` (none/pending_sent/pending_received/accepted) bằng 1 query batch
- [x] T014 [US1] Sửa `web/src/pages/social/FriendsPage.tsx` + `web/src/services/socialApi.ts`: nút theo relationshipStatus ("Kết bạn"/"Đã gửi lời mời"/"Chấp nhận–Từ chối"/"Bạn bè") + tạo test `web/src/pages/social/FriendsPage.test.tsx`

**Checkpoint**: US1 hoàn chỉnh — quickstart.md kịch bản 1 chạy đúng

---

## Phase 4: User Story 2 - Feed thành tích bạn bè (Priority: P1)

**Goal**: Phát `streak_milestone` + `new_pr` (không spam), lọc 7 ngày, hiển thị trên web (FR-005, FR-006)

**Independent Test**: Hoàn thành buổi thứ 3 trong tuần → bạn bè thấy milestone ≤1 phút; phá kỷ lục volume → thấy PR; không có sự kiện trùng; không có sự kiện >7 ngày

### Tests for User Story 2

- [x] T015 [P] [US2] Unit test `backend/src/test/java/com/workoutsmart/social/service/ActivityFeedServiceTest.java` — dedupe milestone theo giá trị streak, mốc 10/30/50/100, dedupe PR theo volume, không emit cho buổi thường
- [x] T016 [P] [US2] Integration test: completeSession → friend thấy event — `backend/src/test/java/com/workoutsmart/feed/controller/SocialFeedControllerIntegrationTest.java`

### Implementation for User Story 2

- [x] T017 [US2] Tạo `ActivityFeedService.java` (backend/.../social/service/): publishStreakMilestone + publishPr (idempotent theo event gần nhất — R7)
- [x] T018 [US2] Sửa `ProfileService.completeSession` (backend/.../profile/service/ProfileService.java): tính streak mới + tổng volume, gọi ActivityFeedService trực tiếp (bọc try/catch log warn, KHÔNG rollback buổi tập). Ghi chú: bỏ phương án AFTER_COMMIT event vì transaction trong callback afterCommit không commit (Spring quirk — đã kiểm chứng, cập nhật research R7)
- [x] T019 [US2] Sửa `ActivityFeedRepository.java` (backend/.../social/repository/) thêm query 7 ngày; `SocialService.feed` dùng query mới
- [x] T020 [US2] Web: panel "Hoạt động bạn bè" gọi `socialApi.feed()` với polling 60s — `web/src/pages/UserDashboard.tsx` + test render theo actionType (`web/src/pages/UserDashboard.test.tsx`)

**Checkpoint**: US2 hoàn chỉnh — quickstart.md kịch bản 2 chạy đúng

---

## Phase 5: User Story 3 - Bảng xếp hạng đúng luật và luôn ghim vị trí của tôi (Priority: P1)

**Goal**: Tie-break theo tuần bắt đầu chuỗi, ghim viewer ngoài top 100, batch ≤5 phút, đọc nhanh (FR-007, FR-008, FR-009)

**Independent Test**: 2 user cùng streak → người đạt chuỗi sớm hơn xếp trên; viewer rank >100 vẫn được ghim; leaderboard phản ánh streak mới ≤5 phút; không còn full-scan mỗi request

### Tests for User Story 3

- [x] T021 [P] [US3] Mở rộng `backend/src/test/java/com/workoutsmart/social/service/StreakCalculatorTest.java` — streakStartWeek đúng, >1000 session không cắt thiếu, tuần hiện tại chưa đủ 3 buổi không đứt chuỗi
- [x] T022 [P] [US3] Integration test: tie-break 2 user cùng streak + viewer ngoài top 100 vẫn được trả — `backend/src/test/java/com/workoutsmart/social/controller/SocialControllerIntegrationTest.java`

### Implementation for User Story 3

- [x] T023 [US3] Sửa `StreakCalculator.java` (backend/.../social/service/): trả thêm `streakStartWeek`, bỏ cap 1000 session (đếm theo tuần — R8)
- [x] T024 [US3] Tạo `LeaderboardSyncService.java` (backend/.../social/service/): `updateEntry(userId)` incremental + job `@Scheduled(fixedDelay=5min)` recompute user có thay đổi + gán `rank`; gọi từ completeSession
- [x] T025 [US3] Sửa `SocialService.computeLeaderboard` thành read-only (đọc `leaderboard_entries`, top 100 + row viewer); `SocialController.leaderboard()` nhận `Authentication`; `LeaderboardRepository` thêm query top theo (currentStreakWeeks DESC, streakStartWeek ASC, userId ASC)
- [x] T026 [US3] Sửa `web/src/pages/social/LeaderboardPage.tsx`: xử lý row viewer ngoài top-100 (pin cuối bảng) + test `web/src/pages/social/LeaderboardPage.test.tsx`

**Checkpoint**: US3 hoàn chỉnh — quickstart.md kịch bản 3 chạy đúng ✅ (283 backend + 61 frontend tests pass)

---

## Phase 6: User Story 4 - Thử thách có vòng đời và tự tổng kết (Priority: P1)

**Goal**: Challenge `open/closed/finished`, scheduler tổng kết ≤1 phút sau end_date, final_rank = streak tại end_date, endpoint kết quả (FR-010, FR-011, FR-012)

**Independent Test**: Admin tạo challenge 1 ngày → 2 user join → qua end_date → tự tổng kết ≤1 phút, mỗi người có final_rank, xem được kết quả

### Tests for User Story 4

- [x] T027 [P] [US4] Unit test `backend/src/test/java/com/workoutsmart/social/service/ChallengeFinalizerTest.java` — idempotent (chạy 2 lần không đổi), xếp hạng theo streak tại end_date + tie-break FR-007
- [x] T028 [P] [US4] Integration test: join hết hạn 422, auto-finalize, results — `backend/src/test/java/com/workoutsmart/social/controller/SocialControllerIntegrationTest.java`

### Implementation for User Story 4

- [x] T029 [US4] Tạo `ChallengeFinalizer.java` (backend/.../social/service/): `@Scheduled(fixedDelay=60s)`; transition `open → closed → finished` bằng UPDATE guard (idempotent, R9)
- [x] T030 [US4] Sửa `SocialService` (createChallenge validation startDate/duration, joinChallenge guard status+end_date, thêm `results(challengeId)`), `SocialController` (+GET /challenges/{id}/results), `ChallengeResponse.java` (+completedAt, finalRank), `CreateChallengeRequest.java` (+Bean Validation) — backend/.../social/
- [x] T031 [US4] Web: hiển thị kết quả chung cuộc cho challenge finished (`web/src/pages/social/LeaderboardPage.tsx`) + form tạo challenge cho admin (`web/src/pages/admin/`)

**Checkpoint**: US4 hoàn chỉnh — quickstart.md kịch bản 4 chạy đúng ✅ (283 backend + 61 frontend tests pass)

---

## Phase 7: User Story 5 - Hồ sơ riêng tư (Priority: P2)

**Goal**: `is_private` (001 sở hữu, mặc định true) — người lạ chỉ thấy display_name + rank (FR-013)

**Independent Test**: User B private → người lạ A không thấy email/bài đăng tường cá nhân; bài public của B vẫn xem được; bạn bè xem bình thường

### Tests for User Story 5

- [x] T032 [P] [US5] Unit test: search ẩn email/avatar cho người lạ khi target private; feed ẩn bài friends/private của user private với người lạ — `backend/src/test/java/com/workoutsmart/social/service/SocialServiceSearchPrivacyTest.java` + `backend/src/test/java/com/workoutsmart/feed/service/SocialFeedServicePrivacyTest.java` + `backend/src/test/java/com/workoutsmart/profile/service/ProfileServicePrivacyTest.java`

### Implementation for User Story 5

- [x] T033 [US5] Sửa `User.java` (backend/.../auth/entity/) khớp `isPrivate`; cập nhật DTO/service profile để đọc/ghi `isPrivate` — `ProfileResponse.java` (+isPrivate), `UpdateProfileRequest.java` (+isPrivate), `ProfileService.java` (+toggle 24h restriction)
- [x] T034 [US5] Sửa `SocialService.searchUsers` (ẩn email cho người lạ khi target private, relationshipStatus="private") + `SocialFeedService.feed`/`CommunityPostRepository` (discover query exclude private users) — backend/.../social/, backend/.../feed/
- [x] T035 [US5] Web: toggle "Hồ sơ riêng tư" — `ProfilePage.tsx` (toggle switch + 24h debounce warning), `profileApi.ts` (+isPrivate), `socialApi.ts` (+private relationshipStatus)

**Checkpoint**: US5 hoàn chỉnh — quickstart.md kịch bản 5 (phần privacy) chạy đúng ✅ (295 backend + 61 frontend tests pass)

---

## Phase 8: User Story 6 - Đăng bài với ảnh và GIF embed (Priority: P2)

**Goal**: Upload chỉ ảnh PNG/JPG/JPEG/WEBP ≤10MB (magic bytes); gifUrl Tenor (iframe) / Instagram (link+preview) (FR-014, FR-015)

**Independent Test**: Upload ảnh OK; upload GIF/video (kể cả đổi đuôi .jpg) bị 400; gifUrl tenor.com OK, instagram.com OK (link), domain khác 400

### Tests for User Story 6

- [ ] T036 [P] [US6] Mở rộng `backend/src/test/java/com/workoutsmart/feed/service/SeaweedStorageServiceTest.java` — magic bytes JPEG/PNG/WEBP pass, GIF/video/video-đổi-đuôi fail, >10MB fail
- [ ] T037 [P] [US6] Integration test: POST /feed/posts với media GIF/video → 400; gifUrl ngoài allowlist → 400; tenor.com → 201 — `backend/src/test/java/com/workoutsmart/feed/controller/SocialFeedControllerIntegrationTest.java`

### Implementation for User Story 6

- [ ] T038 [US6] Sửa `SeaweedStorageService.java` (backend/.../feed/service/): bỏ `gif` khỏi danh sách cho phép, thêm kiểm tra magic bytes ≤512 bytes đầu (R6)
- [ ] T039 [US6] Sửa `SocialFeedService.createPost` + `PostResponse.java`: nhận/validate `gifUrl` (https + host allowlist tenor.com/instagram.com), trả trong response — backend/.../feed/
- [ ] T040 [US6] Sửa `web/src/pages/social/CommunityFeedPage.tsx` + `web/src/services/feedApi.ts`: xóa dead-code render video, thêm ô dán link GIF, render Tenor iframe / Instagram link+preview + cập nhật test `CommunityFeedPage.test.tsx`

**Checkpoint**: US6 hoàn chỉnh — quickstart.md kịch bản 5 (phần media) chạy đúng

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Đồng bộ artifacts cũ, gate chất lượng toàn diện

- [ ] T041 [P] Cập nhật `specs/003-social-community/` (spec.md FR-011 auto-accept, status challenge, FR-012b media; data-model.md; contracts/openapi.yaml) — đánh dấu 018 là bản sửa đổi chính thức (supersede các mục mâu thuẫn)
- [ ] T042 [P] Chạy speckit-analyze trên 018 (spec/plan/tasks consistency) — báo cáo read-only, 0 CRITICAL/HIGH chưa xử lý
- [ ] T043 Chạy `mvn test` + kiểm tra coverage ≥80% phần code đổi (jacoco) — backend
- [ ] T044 Chạy `npm test` + `npm run build` + ESLint — web
- [ ] T045 Chạy toàn bộ `specs/018-fix-social-flows/quickstart.md` end-to-end trên môi trường local
- [ ] T046 Whole-plan consistency sweep (đọc lại spec/plan/tasks, không còn thuật ngữ cũ: upcoming/active/completed, GIF upload, workout_completed emit) + commit theo Conventional Commits trên `feature/fix-social-flows`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (P1)**: không phụ thuộc — bắt đầu ngay
- **Foundational (P2)**: phụ thuộc Setup — CHẶN mọi user story
- **User Stories (P3–P8)**: phụ thuộc Foundational; có thể chạy song song (P1 trước, P2 sau)
- **Polish (P9)**: phụ thuộc mọi story mong muốn hoàn thành

### User Story Dependencies

- **US1 (P1)**: sau Foundational — không phụ thuộc story khác (MVP)
- **US2 (P1)**: sau Foundational; dùng FriendshipRepository (US1) cho feed bạn bè → khuyến nghị sau US1
- **US3 (P1)**: sau Foundational; dùng StreakCalculator — độc lập với US1/US2
- **US4 (P1)**: sau Foundational; dùng StreakCalculator + tie-break US3 → khuyến nghị sau US3
- **US5 (P2)**: sau Foundational + V22 — độc lập
- **US6 (P2)**: sau Foundational + V23 — độc lập

### Within Each User Story

- Tests viết TRƯỚC và phải FAIL trước khi implement
- Migration/entity trước service; service trước endpoint; core trước integration
- Story xong (checkpoint) trước khi sang story kế tiếp

### Parallel Opportunities

- T002/T003 [P] song song; T005/T006/T007 [P] song song sau T004
- Sau Foundational: US3 và US5+US6 chạy song song độc lập; US1/US2 một luồng; US4 sau US3
- Test task [P] trong cùng story chạy song song

---

## Parallel Example: User Story 3

```bash
# Chạy cùng lúc (khác file):
Task: "Mở rộng StreakCalculatorTest.java — streakStartWeek + >1000 sessions"
Task: "Integration test tie-break + pin viewer ngoài top 100 trong SocialControllerIntegrationTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup (T001–T003)
2. Phase 2: Foundational (T004–T008) — CRITICAL
3. Phase 3: US1 — STOP & VALIDATE (kịch bản 1 quickstart)
4. Demo/deploy nếu sẵn sàng

### Incremental Delivery

1. Setup + Foundational → nền xong
2. +US1 (kết bạn) → test → MVP
3. +US2 (feed) → test
4. +US3 (leaderboard) → test
5. +US4 (challenge) → test
6. +US5 (privacy) → test
7. +US6 (media/GIF) → test
8. Phase 9: gates + artifacts đồng bộ

### Parallel Team Strategy

- Dev A: US1 → US2 (luồng bạn bè/feed)
- Dev B: US3 → US4 (luồng streak/challenge)
- Dev C: US5 + US6 (luồng privacy/media) — cần Foundational trước

---

## Notes

- [P] tasks = khác file, không phụ thuộc
- [Story] label map task về user story để traceability
- Mỗi story độc lập hoàn thành + test được
- Test viết trước, xác nhận FAIL rồi mới implement
- Commit theo Conventional Commits (subject ≤ 50 ký tự) sau mỗi task/nhóm logic
- KHÔNG sửa migration V7/V19 — chỉ tạo V20–V23
- Constitution amendment (T002) là gate của toàn bộ feature — làm đầu tiên
- git-flow-next chưa cài trên máy (đang dùng `feature/fix-social-flows` bằng git thuần) — ghi chú khi finish
