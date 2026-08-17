# Tasks: Theo dõi buổi tập (009-workout-tracking)

## Phase 1: Setup
- [x] T001 Tạo DTO tracking + thêm `WorkoutSetRepository.findBySessionIdAndSetNumber`

## Phase 2: Session lifecycle
- [x] T002 `TrackingService.startSession/recordSet/incrementFocus/expireStaleSessions`
- [x] T003 `TrackingController` (`POST /workout-sessions`, `POST /workout-sessions/{id}/sets`, `POST /workout-sessions/{id}/focus-interruption`)

## Phase 3: Polish
- [x] T004 Test unit `TrackingServiceTest` + integration `TrackingControllerIntegrationTest`
- [x] T005 Chạy `mvn test` + coverage
