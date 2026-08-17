# Implementation Plan: Hồ sơ & Lịch sử tập (001-profile-history)

**Branch**: `feature/001-profile-history` | **Date**: 2026-08-17 | **Spec**: [spec.md](spec.md)

## Summary

Cung cấp endpoint xem/sửa hồ sơ cá nhân (không sửa cân nặng), xóa tài khoản soft-delete, và lịch sử buổi tập phân trang kèm chi tiết hiệp. Backend Spring Boot (package `com.workoutsmart.profile`), web React 2 trang (Profile, History). Tái sử dụng entity `User` (auth) + migration V4 (workout_sessions/sets đã có).

## Technical Context

- **Language**: Java 17, Spring Boot 3.3, Spring Data JPA
- **Storage**: PostgreSQL 18 — tái sử dụng `users` (V1), `workout_sessions`/`workout_sets` (V4); không migration mới
- **Testing**: JUnit 5 + Mockito + MockMvc integration (H2 + Flyway)
- **Auth**: mọi endpoint yêu cầu JWT; lấy userId từ SecurityContextHolder

## Constitution Check

| Nguyên tắc | Đánh giá |
|---|---|
| Layered Controller→Service→Repository | ✅ |
| Bean Validation + HTTP status chuẩn | ✅ |
| Không raw SQL; không migration mới (tái sử dụng) | ✅ |
| Test ≥80% service | ✅ |
| Git Flow + Conventional Commits | ✅ |

**GATE: PASS**

## Project Structure

```text
backend/src/main/java/com/workoutsmart/profile/   # controller, service, dto, entity, repository
backend/src/test/java/com/workoutsmart/profile/   # unit + integration
web/src/pages/profile/                             # ProfilePage, WorkoutHistoryPage
web/src/services/profileApi.ts
specs/001-profile-history/                         # plan.md, data-model.md, contracts/, tasks.md
```
