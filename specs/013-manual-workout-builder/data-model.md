# Data Model: Tự xây lộ trình (picker)

Không entity / bảng mới cho User Story 4.

Picker dùng lại:

- **Exercise** (kho + custom): `name`, `category`, `equipment`, `image`, `gif_url`, `instructions`, `source`
- **LibraryFilter / ExercisePickerFilter**: tập Category + tập Equipment + từ khóa tên (cùng semantics 012)
- **PlanExercise**: bản ghi template ngày sau khi User xác nhận thêm

Giới hạn: tối đa 15 `PlanExercise` mỗi `PlanDay` (không đổi).
