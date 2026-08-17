# Tasks: Quản trị hệ thống (006-admin-management)

## Phase 1: Setup
- [x] T001 Tạo DTO `AdminUserResponse`, `BanUserRequest`, `CreateExerciseRequest`, `UpdateExerciseRequest`, `ExerciseImportRequest`, `ExerciseImportResponse` trong `admin/dto`

## Phase 2: User management (US1)
- [x] T002 Mở rộng `AdminService`: `searchUsers(q)`, `getUserDetail(id)`, `banUser(id, reason, adminId)`, `unbanUser(id, adminId)`
- [x] T003 Mở rộng `AdminController`: `GET /admin/users`, `GET /admin/users/{id}`, `POST /admin/users/{id}/ban`, `POST /admin/users/{id}/unban`

## Phase 3: Exercise management (US2)
- [x] T004 Mở rộng `AdminService`: `createExercise`, `updateExercise`, `importExercises`
- [x] T005 Mở rộng `AdminController`: `POST /admin/exercises`, `PUT /admin/exercises/{id}`, `POST /admin/exercises/import`
- [x] T006 Test unit `AdminServiceTest` + integration `AdminControllerIntegrationTest`

## Phase 4: Polish
- [x] T007 Chạy `mvn test` + coverage
