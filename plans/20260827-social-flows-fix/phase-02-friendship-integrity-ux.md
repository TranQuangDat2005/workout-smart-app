---
phase: 2
title: "Friendship Integrity & UX"
status: pending
priority: P1
dependencies: [1]
---

# Phase 2: Friendship Integrity & UX

## Overview
Sửa data integrity + race condition của `friendships` (C1) và UX tìm kiếm theo trạng thái quan hệ (M1, AS7), enforce cap 500 bạn bè (M2).

## Requirements
- Functional (EARS): WHERE 2 user gửi lời mời đồng thời, hệ thống PHẢI chỉ tồn tại ĐÚNG 1 row friendship cho cặp (user_id_1, user_id_2) — không sinh row trùng, không 500.
- WHEN gửi lại lời mời sau khi bị từ chối (đã hết cooldown 30 ngày), hệ thống PHẢI tái sử dụng row cũ thay vì tạo row mới.
- WHERE User đã đạt 500 bạn bè (accepted), hệ thống PHẢI chặn gửi/nhận thêm lời mời mới (HTTP 422).
- Search results PHẢI kèm trạng thái quan hệ để UI hiển thị nút đúng (Kết bạn / Đang chờ phản hồi / Chấp nhận–Từ chối / Đã là bạn).
- Q1 (auto-accept) GIỮ NGUYÊN hành vi code — không sửa logic cross-invite; chỉ sửa spec ở Phase 1.
- Non-functional: không 500 khi có >1 row kế thừa; migration mới (V20) — KHÔNG sửa V7.

## Architecture
- DB: migration `V20__friendship_unique.sql` — normalize cặp (least, greatest) + `CREATE UNIQUE INDEX uq_friendships_pair ON friendships (LEAST(user_id_1,user_id_2), GREATEST(user_id_1,user_id_2))`; trước đó dedupe data cũ (giữ row accepted > pending > rejected mới nhất).
- Service: tách logic `sendRequest` thành các bước guard (self-friend → 500-cap → findBetween chịu lỗi → cooldown → rate limit) + REUSE row khi rejected.
- `FriendshipRepository.findBetween` đổi từ `Optional<Friendship>` sang `List<Friendship>` (hoặc query `findFirst`) để không ném `IncorrectResultSizeDataAccessException`.
- DTO: `UserSearchResponse` thêm `relationshipStatus` (none/pending_sent/pending_received/accepted).

## Related Code Files
- Create: `backend/src/main/resources/db/migration/V20__friendship_unique.sql`
- Modify: `backend/.../social/service/SocialService.java` (sendRequest, accept, unfriend, searchUsers)
- Modify: `backend/.../social/repository/FriendshipRepository.java`
- Modify: `backend/.../social/dto/UserSearchResponse.java`, `FriendshipResponse.java`
- Modify: `web/src/pages/social/FriendsPage.tsx`, `web/src/services/socialApi.ts`

## Implementation Steps
1. Viết V20: dedupe + unique index (least/greatest); verify trên DB local.
2. Sửa `findBetween` → không ném khi nhiều row; `sendRequest` reuse row rejected thay vì save mới.
3. Thêm cap 500 bạn (đếm accepted trước khi accept/gửi — 422 khi vượt).
4. `accept()` thêm guard `status == pending` (409 nếu không).
5. Bổ sung `relationshipStatus` vào search response + service.
6. Web: FriendsPage render nút theo trạng thái (Chấp nhận/Từ chối cho pending_received; "Đã gửi" cho pending_sent; "Bạn bè" cho accepted).
7. Test: unit (sendRequest các nhánh, reuse row, cap 500) + integration (2 request đồng thời → 1 row).

## Success Criteria
- [ ] 2 request đồng thời ngược chiều → đúng 1 row trong DB, không exception.
- [ ] Gửi lại sau cooldown → vẫn 1 row/pair; không phát sinh duplicate.
- [ ] Cap 500 bạn trả 422 đúng thông điệp.
- [ ] Search UI hiển thị đúng nút theo trạng thái (AS7).
- [ ] `mvn test` pass; coverage phần sửa ≥ 80%.

## Risk Assessment
- Dedupe data cũ chọn sai row (giữ rejected thay vì accepted) → rule ưu tiên accepted > pending > rejected; test migration trên bản sao DB trước.
- Unique index trên expression `LEAST/GREATEST` với PostgreSQL 18: OK; lưu ý ứng dụng vẫn phải normalize thứ tự khi insert để code nhất quán.
