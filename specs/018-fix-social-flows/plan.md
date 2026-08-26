# Implementation Plan: Sửa luồng Xã hội & Cộng đồng (018-fix-social-flows)

**Branch**: `feature/fix-social-flows` | **Date**: 2026-08-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification từ `specs/018-fix-social-flows/spec.md` + quyết định owner (Clarifications 2026-08-27) + khuyến nghị ck-predict.

## Summary

Sửa toàn bộ luồng xã hội hiện có (package `com.workoutsmart.social` + `com.workoutsmart.feed`) cho khớp spec 003 + constitution: (1) bất biến 1 quan hệ bạn bè/pair với unique index một phần + trạng thái `superseded`, tái dùng bản ghi sau cooldown, cap 500 bạn, trạng thái quan hệ trong search; (2) feed thành tích (streak_milestone + new_pr, 7 ngày) publish sau commit, không spam; (3) leaderboard tie-break theo tuần bắt đầu chuỗi (`streak_start_week`), ghim vị trí cá nhân ngoài top 100, batch 5 phút; (4) challenge vòng đời `open/closed/finished` + scheduler tổng kết (final_rank = streak tại end_date); (5) privacy `is_private` (001 sở hữu, mặc định private); (6) media: ảnh upload (magic bytes, không GIF/video) + GIF embed Tenor (Instagram link/preview). Migration mới V20–V23, KHÔNG sửa migration cũ.

## Technical Context

**Language/Version**: Java 17 + Spring Boot 3.3 (backend); TypeScript strict + React 18 + Vite (web)

**Primary Dependencies**: Spring Data JPA/Hibernate, Spring Security (JWT), Bean Validation, Flyway, Lombok; Axios, Jest + React Testing Library (web)

**Storage**: PostgreSQL 18; media ảnh trên SeaweedFS self-hosted (đã có); GIF embed là URL bên thứ 3 (Tenor/Instagram) — backend không gọi external API

**Testing**: JUnit 5 + Mockito (unit) + MockMvc (integration); Jest + RTL (web)

**Target Platform**: Backend REST API + Web SPA (mobile Flutter chưa làm đợt này)

**Project Type**: web-service + web-app (monorepo `backend/` + `web/`)

**Performance Goals**: leaderboard read < 200ms với 10k user (SC-004); feed event hiển thị ≤ 1 phút (SC-002); challenge tổng kết ≤ 1 phút sau end_date (SC-003); `completeSession` không tăng đáng kể độ trễ (publish sau commit)

**Constraints**: constitution §4 (streak định nghĩa DUY NHẤT), §5 (external API allowlist), §7 (không sửa/xóa migration cũ, cleanup không xóa business data), §9 (coverage ≥ 80% phần code đổi), §10 (Git Flow, không commit thẳng main); ≤ 5 lời mời/ngày; cooldown 30 ngày; feed 7 ngày; cap 50 items

**Scale/Scope**: ~10k users giả định; top 100 leaderboard + row viewer; max 500 bạn/user; 1000+ session/user phải tính streak đúng

## Constitution Check

| Nguyên tắc | Đánh giá |
|---|---|
| Streak định nghĩa DUY NHẤT (constitution §4) | ✅ StreakCalculator dùng chung; challenge xếp hạng theo streak tại end_date (Clarifications) |
| Leaderboard vô tận + Challenge có hạn | ✅ open/closed/finished + scheduler tổng kết |
| External API allowlist (§5) | ⚠️ CẦN AMENDMENT v2.4.0: thêm "Tenor embed (GIF bài đăng — URL do User cung cấp, chỉ Web client) + Instagram link/preview" vào allowlist — task riêng theo Governance §11 (PR + cập nhật AGENTS.md) |
| Privacy | ✅ is_private (001 sở hữu, mặc định private) — người lạ chỉ thấy display_name + rank |
| Migration lifecycle (§7) | ✅ V20–V23 additive; dedupe bằng `superseded` KHÔNG xóa row; không sửa V7/V19 |
| Layered + Bean Validation + controller mỏng | ✅ |
| Test ≥ 80% + build pass | ✅ (Phase 7 của kế hoạch tổng) |

**GATE: PASS** (có điều kiện: amendment constitution v2.4.0 phải được tạo trong task đầu tiên)

## Project Structure

### Documentation (this feature)

```text
specs/018-fix-social-flows/
├── plan.md              # file này
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/           # Phase 1 (openapi.yaml)
├── checklists/          # speckit-specify output
└── tasks.md             # Phase 2 (speckit-tasks — tạo sau)
```

### Source Code (repository root)

```text
backend/src/main/java/com/workoutsmart/
├── social/            # SỬA: entity, repository, service, controller, dto
│   ├── entity/        # Friendship(+superseded), LeaderboardEntry(+streakStartWeek), ActivityFeedItem, Challenge, ChallengeParticipant
│   ├── repository/    # FriendshipRepository (findBetween→List), ActivityFeedRepository(+7d), LeaderboardRepository(+top)
│   ├── service/       # SocialService, StreakCalculator, +ActivityFeedService, +ChallengeFinalizer, +LeaderboardSyncService
│   └── controller/    # SocialController (leaderboard nhận auth; +results endpoint)
├── feed/              # SỬA: SeaweedStorageService (magic bytes, bỏ gif), SocialFeedService (gifUrl, privacy), CommunityPost(+gifUrl)
├── profile/           # SỬA: ProfileService.completeSession → publish events (sau commit)
└── auth/entity/       # SỬA: User + is_private (field do 001 sở hữu — migration V22 ở feature này)

backend/src/main/resources/db/migration/
├── V20__friendship_unique.sql          # dedupe + superseded + unique index một phần
├── V21__leaderboard_streak_start.sql   # streak_start_week
├── V22__user_is_private.sql            # users.is_private default true
└── V23__community_post_gif_url.sql     # community_posts.gif_url

backend/src/test/java/com/workoutsmart/{social,feed}/  # bổ sung unit + integration

web/src/
├── pages/social/      # FriendsPage (nút theo trạng thái), LeaderboardPage (pin + results), CommunityFeedPage (gif embed, bỏ video dead-code)
├── pages/profile/     # toggle is_private (001)
├── pages/admin/       # form tạo challenge
└── services/          # socialApi, feedApi (gifUrl, relationshipStatus)
```

**Structure Decision**: Giữ nguyên cấu trúc package hiện có (social + feed tách riêng, constitution §3 phân lớp). Thêm 3 service mới trong `social` (ActivityFeedService, ChallengeFinalizer, LeaderboardSyncService) thay vì nhồi vào SocialService. Chiều phụ thuộc module duy nhất: profile → social (KHÔNG có chiều ngược).

## Complexity Tracking

Không có vi phạm constitution cần biện minh. (Amendment §5 là mở rộng allowlist có phê duyệt owner, không phải vi phạm.)
