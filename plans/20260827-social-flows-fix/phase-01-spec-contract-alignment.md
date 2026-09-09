---
phase: 1
title: "Spec & Contract Alignment"
status: pending
priority: P1
dependencies: []
---

# Phase 1: Spec & Contract Alignment

## Overview
Ghi nhận 6 quyết định owner (Decision Log — Q1–Q5 đã chốt 2026-08-27, Q6 dùng assumption) và đồng bộ lại `specs/003-social-community/spec.md`, `data-model.md`, `contracts/openapi.yaml`, `specs/General Spec.md`, constitution + AGENTS.md cho khớp — TRƯỚC khi sửa code (speckit: spec trước code).

## Requirements
- Functional: mỗi FR sửa đổi viết theo EARS (WHEN/WHERE/… SHALL), tiếng Việt.
- Non-functional: không mâu thuẫn constitution (§4 streak duy nhất, §5 allowlist, §10 git flow); không xóa migration; sửa constitution phải theo Governance §11 (PR riêng + review AGENTS.md).

## Architecture
Không đổi kiến trúc. Chỉ chuẩn hóa artifact theo Decision Log:
- Q1: FR-011/AS7 sửa thành auto-accept (khớp code, data-model, openapi).
- Q2: FR-012b sửa thành "upload chỉ ảnh (PNG/JPG/JPEG/WEBP — KHÔNG GIF/video); GIF qua embed Tenor/Instagram (URL do User cung cấp, chỉ Web client, backend không gọi external)". Constitution §5 thêm Tenor/Instagram embed (pattern giống YouTube/Spotify) → amendment v2.4.0.
- Q3: FR-010 ghi rõ tie-break = ai đạt streak hiện tại sớm hơn; Key Entities thêm `streak_start_week`.
- Q4: FR-013/014/015 đổi từ vựng status sang `open/closed/finished`; đồng bộ General Spec §5 + data-model.md + UI labels.
- Q5: FR-005 ghi rõ feed chỉ hiển thị `streak_milestone` + `new_pr` (KHÔNG emit `workout_completed` — giá trị vẫn giữ trong enum dữ liệu General Spec nhưng không dùng, ghi chú).
- Q6 (assumption A+C): `is_private` do 001-profile-history sở hữu, mặc định private.

## Related Code Files
- Modify: `specs/003-social-community/spec.md` (Decision Log + FR-005/010/011/012/012b/013/014/015 + Out of Scope)
- Modify: `specs/003-social-community/data-model.md`, `specs/003-social-community/contracts/openapi.yaml`
- Modify: `specs/General Spec.md` (status challenge + ghi chú action_type không emit)
- Modify: `.specify/memory/constitution.md` (amendment v2.4.0 — Tenor/Instagram embed), `AGENTS.md` (allowlist)

## Implementation Steps
1. Thêm section "Decision Log" vào `spec.md` ghi Q1–Q6 + ngày chốt.
2. Sửa FR-011 + AS7 → auto-accept (bỏ "nút chuyển thành Chấp nhận/Từ chối", thay bằng "hệ thống tự động chấp nhận và tạo quan hệ hai chiều").
3. Sửa FR-012b + Key Entities `Community Post` → ảnh upload (không GIF/video) + `gif_url` (embed Tenor/Instagram, nullable).
4. Sửa FR-010 + Key Entities `Leaderboard Entry` → thêm `streak_start_week`.
5. Sửa FR-013/015 → status `open/closed/finished`; cập nhật General Spec §5 bảng challenges cho khớp.
6. Sửa FR-005 → chỉ streak_milestone + new_pr; ghi chú `workout_completed` không emit.
7. Thêm "Out of Scope": block user, realtime feed, upload video/GIF lên SeaweedFS.
8. Constitution amendment v2.4.0 (Tenor/Instagram embed — chỉ Web client) + đồng bộ AGENTS.md allowlist; PR riêng theo Governance §11.
9. Cập nhật openapi.yaml: bổ sung `/leaderboard/friends`, toàn bộ `/api/v1/feed/**`, `gif_url` trên Post, response UserSearch có `relationshipStatus`, status challenge mới.
10. Commit spec trước (`docs: align 003 spec with owner decision log`).

## Success Criteria
- [ ] Decision Log Q1–Q5 ghi rõ trong spec.md; Q6 ghi assumption (chờ confirm).
- [ ] Không còn mâu thuẫn từ vựng status challenge giữa 3 artifacts.
- [ ] openapi.yaml liệt kê đủ endpoint hiện có + sẽ thêm.
- [ ] Constitution v2.4.0 + AGENTS.md đồng bộ (Q2).
- [ ] speckit-analyze chạy lại trên 003 không còn CRITICAL về consistency.

## Risk Assessment
- Q6 chưa chốt → code Phase 6 vẫn làm theo assumption (cấu trúc field không đổi dù default khác); nếu owner chọn khác chỉ đổi default + spec sở hữu.
- Sửa constitution (Q2) có blast radius lớn → PR riêng + review templates phụ thuộc (AGENTS.md), không gộp chung commit code.
