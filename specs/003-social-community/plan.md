# Implementation Plan: Xã hội & Cộng đồng (003-social-community)

**Branch**: `feature/003-social-community` | **Date**: 2026-08-17 | **Spec**: [spec.md](spec.md)

## Summary

Tìm kiếm user (privacy: email chỉ lộ cho bạn bè), kết bạn với rate limit/cooldown/xử lý lời mời chéo, feed bạn bè, leaderboard streak vô tận (định nghĩa streak duy nhất: chuỗi tuần ≥3 buổi), Challenge có thời hạn (Admin tạo, user tham gia). Package `com.workoutsmart.social`, migration V7 (đã tạo).

## Constitution Check

| Nguyên tắc | Đánh giá |
|---|---|
| Layered + Bean Validation | ✅ |
| Streak định nghĩa DUY NHẤT | ✅ StreakCalculator dùng chung |
| Leaderboard vô tận + Challenge | ✅ |
| Privacy (chỉ hiện display_name + rank cho người lạ) | ✅ |
| Test ≥80% | ✅ |

**GATE: PASS**
