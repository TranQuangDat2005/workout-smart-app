---
phase: 6
title: "Privacy & Media Policy"
status: pending
priority: P2
dependencies: [1]
---

# Phase 6: Privacy & Media Policy

## Overview
Enforce FR-012 (hồ sơ private — H4) và chính sách media theo **Q2 đã chốt**: upload chỉ ảnh (PNG/JPG/JPEG/WEBP — KHÔNG GIF/video), GIF dùng embed hệ thống ngoài (Tenor/Instagram, URL do User cung cấp, chỉ Web client). Hiện `users` không có field `is_private`; code cho phép GIF upload; web có dead-code render video.

## Requirements
- Functional (theo Q6 assumption A+C): WHERE hồ sơ private, người lạ CHỈ xem display_name + rank, không xem tường cá nhân/bài đăng; bài đăng `audience=public` vẫn ai cũng xem (FR-012).
- Media (Q2): upload CHỈ ảnh PNG/JPG/JPEG/WEBP ≤ 10MB — từ chối GIF/video (400, thông điệp rõ); kiểm tra cả extension lẫn magic bytes (không chỉ tên file). Bài đăng có thể kèm `gif_url` (nullable): URL https thuộc host allowlist `tenor.com` / `instagram.com`; backend CHỈ lưu chuỗi, KHÔNG gọi external API; Web client embed (iframe/oembed) — giống pattern YouTube/Spotify trong constitution.
- Non-functional: không lộ email người lạ (đã đúng — giữ); media endpoint giữ public redirect (v1, key UUID) kèm ghi chú rủi ro.

## Architecture
- Field `is_private` (boolean, default **private** theo General Spec §3) thêm vào `users` (migration mới V22). Spec sở hữu: 001-profile-history (Q6=A) — 003 chỉ consume.
- Social consumption: `searchUsers` (ẩn email cho người lạ khi target private), `SocialFeedService.feed` + `CommunityPostRepository.findFeed/findDiscover` (bài của user private: chỉ hiện cho bạn bè; bài `public` của user private vẫn hiện theo FR-012 "bài đăng public ai cũng xem" — xác nhận lại khi implement, mặc định giữ FR-012), profile view (001) tự enforce tường cá nhân.
- Media: `SeaweedStorageService.classify` bỏ `gif` khỏi `IMAGE_EXTENSIONS`, thêm kiểm tra magic bytes (đọc ≤ 512 bytes đầu); `CommunityPost` thêm cột `gif_url` (migration V23) + validate host allowlist; web: bỏ nhánh render video (dead code), thêm ô dán link GIF (Tenor/Instagram) + render embed.

## Related Code Files
- Create: migration `V22__user_is_private.sql`, `V23__community_post_gif_url.sql`
- Modify: `backend/.../auth/entity/User.java`, `profile/` (DTO update `is_private` — 001 sở hữu)
- Modify: `backend/.../social/service/SocialService.java` (searchUsers), `feed/service/SocialFeedService.java` (feed query + gif_url), `feed/repository/CommunityPostRepository.java`, `feed/entity/CommunityPost.java`
- Modify: `backend/.../feed/service/SeaweedStorageService.java` (bỏ gif + magic bytes)
- Modify: `web/src/pages/social/CommunityFeedPage.tsx` (bỏ video code, thêm GIF embed), `web/src/pages/profile/*` (toggle private), `web/src/services/feedApi.ts`
- Modify: `specs/003-social-community/spec.md` (FR-012, FR-012b theo Q2), `specs/001-profile-history/spec.md` (Q6=A), constitution + AGENTS.md (Phase 1)

## Implementation Steps
1. Migration V22 thêm `is_private` (default true); profile API expose + update (001).
2. Search: ẩn email cho người lạ nếu target private; UI hiện "Hồ sơ riêng tư".
3. Feed query: bài của user private chỉ hiện cho bạn bè (trừ `audience=public` theo FR-012).
4. Media validation: bỏ GIF upload + magic bytes; migration V23 + validate `gif_url` host allowlist.
5. Web: xóa dead-code video; Composer thêm input link GIF (Tenor/Instagram) + render embed; profile toggle private.
6. Test: unit privacy search/feed, media reject GIF/video upload + reject URL ngoài allowlist; web test Composer.

## Success Criteria
- [ ] User private: người lạ chỉ thấy display_name + rank (search, leaderboard, feed).
- [ ] Upload GIF/video bị từ chối 400; `gif_url` ngoài allowlist bị 400; URL Tenor/Instagram hợp lệ được lưu + hiển thị embed.
- [ ] Không còn dead-code video trên web.
- [ ] `mvn test` + `npm test` pass.

## Risk Assessment
- Thêm field vào bảng `users` (core) — blast radius lớn → migration additive, default an toàn (private), không đổi hành vi các flow khác.
- Embed iframe từ bên thứ 3 (Tenor/Instagram) → chỉ phép https + host allowlist; không render nội dung không rõ nguồn.
- Magic bytes đọc InputStream → đọc tối đa 512 bytes đầu, đóng stream an toàn.
- Constitution sửa (Phase 1) phải đi kèm PR riêng + update AGENTS.md — không gộp chung commit code.
