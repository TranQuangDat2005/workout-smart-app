# Implementation Plan: Quản trị hệ thống (006-admin-management)

**Branch**: `feature/006-admin-management` | **Spec**: [spec.md](spec.md)

## Summary
Backend REST cho Admin: tìm kiếm/xem user, ban/unban (audit + revoke refresh), quản lý bài tập (thêm/sửa/ẩn/import upsert). Không upload media mới — chỉ dùng path từ `exercises-dataset/`.

## Technical Context
- **Stack**: Java 17 + Spring Boot 3.3 + Spring Data JPA + Flyway + PostgreSQL 18 (test: H2).
- **Storage**: reuse `users`, `exercises`, `workout_sessions`, `audit_logs` (V8 đã có). Không migration mới.
- **Auth**: role ADMIN qua SecurityConfig `/api/v1/admin/**`.
- **Audit**: mọi thao tác Admin ghi `audit_logs` (constitution §5).

## Project Structure
```text
backend/src/main/java/com/workoutsmart/admin/
├── entity/AuditLog.java (đã có)
├── repository/AuditLogRepository.java (đã có)
├── dto/ (AdminUserResponse, BanUserRequest, CreateExerciseRequest,
│          UpdateExerciseRequest, ExerciseImportRequest, ExerciseImportResponse,
│          SetExerciseStatusRequest)
├── service/AdminService.java (mở rộng)
└── controller/AdminController.java (mở rộng)
```

## Constitution Check
| Nguyên tắc | Status |
|---|---|
| Phân lớp Controller→Service→Repository | PASS |
| Bean Validation cho endpoint ghi | PASS |
| Audit log mọi thao tác Admin | PASS |
| Không upload media mới | PASS |
| Không raw SQL | PASS |
