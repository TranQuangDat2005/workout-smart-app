# Tasks: Bài tập tính theo thời gian

- [x] T001 Migration `V14__time_based_exercises.sql` (measure_type + duration)
- [x] T002 Entity: `Exercise.measureType`; duration trên plan/session/set entities
- [x] T003 DTO: `durationSeconds`/`targetDurationSeconds`/`measureType` trên request/response
- [x] T004 `WorkoutPlanService`: lưu/trả target duration + measureType
- [x] T005 `TrackingService`: snapshot/record/trả duration + measureType
- [x] T006 `RuleEngineService`: sinh target duration cho cardio/isometric
- [x] T007 `ExerciseDataSeeder`: phân loại measure_type (cardio/stretching/isometric)
- [x] T008 `ExerciseDetailResponse` + web `ExerciseDetail`: trả `measureType`
- [x] T009 Web `WorkoutSession`: input `mm:ss` cho bài duration + hiển thị thời gian
- [x] T010 Web `PlanEditor`: hiển thị/sửa target thời gian cho bài duration
