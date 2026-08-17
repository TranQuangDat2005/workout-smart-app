# Implementation Plan: Theo dõi buổi tập (009-workout-tracking)

**Branch**: `feature/009-workout-tracking` | **Spec**: [spec.md](spec.md)

## Summary
Backend REST cho tracking: start session, ghi hiệp (UPSERT theo session_id+set_number), tăng focus_interruptions, hoàn thành session, auto-expire. Rest Timer & Focus UI là client-side. Timezone dùng `ZoneId.systemDefault()` (MVP).

## Technical Context
- **Stack**: Java 17 + Spring Boot 3.3 + Spring Data JPA + Flyway + PostgreSQL 18.
- **Storage**: reuse `workout_sessions`, `workout_sets` (V4). Không migration mới.
- **Auth**: JWT, endpoint thuộc `/api/v1/workout-sessions/**` (authenticated).

## Project Structure
```text
backend/src/main/java/com/workoutsmart/tracking/
├── dto/ (StartSessionRequest, SessionResponse, RecordSetRequest, SetResponse)
├── service/TrackingService.java
└── controller/TrackingController.java
```
