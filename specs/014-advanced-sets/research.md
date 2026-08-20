# Research: Hiệp tập nâng cao (target từng hiệp & kiểu set)

## 1. "Flex" là gì và đang thiếu ở đâu?

**Decision**: Cần xây target từng hiệp ở **template** (kế hoạch), vì ghi reps/tạ thực tế khác nhau từng hiệp đã có sẵn ở `workout_sets`.

**Rationale**: `workout_sets` mỗi set là một dòng với `reps_completed`/`weight_used` riêng; cái thiếu là *mục tiêu* (target) per set trong `workout_plan_exercises` (hiện chỉ 1 `target_reps` chung).

**Alternatives**: Chỉ flex lúc tập (đã có, không đủ cho pyramid khi lập kế hoạch).

## 2. Lưu target từng hiệp ở template thế nào?

**Decision**: Bảng con `workout_plan_exercise_sets` (chuẩn hóa), KHÔNG dùng JSONB array.

**Rationale**: Query/validate/so sánh target dễ; khớp hướng relational + JPA của project; dễ snapshot sang session.

**Alternatives**: JSONB array trên `workout_plan_exercises` (mất ràng buộc, khó JOIN/thống kê).

## 3. Target và thực tế tách hay gộp?

**Decision**: Tách target và thực tế — mirror mẫu hiện có (`workout_plan_exercises` → `workout_session_exercises` → `workout_sets`). Thêm bảng con `workout_session_exercise_sets` cho target snapshot; `workout_sets` (thực tế) chỉ thêm `set_type`.

**Rationale**: Giữ `workout_sets` là dữ liệu thực tế (offline sync chỉ sync thực tế); target nằm trong snapshot, không bị lẫn với dữ liệu ghi nhận.

**Alternatives**: Gộp target vào `workout_sets` (target null = chưa ghi) — dễ nhầm "set rỗng" với "chưa tập", làm bẩn thống kê.

## 4. Kiểu set (set_type) mô hình thế nào?

**Decision**: Cột `set_type` (chuỗi, default `normal`) trên cả 3 tầng: template set, snapshot set, và `workout_sets` (thực tế). Enum hợp lệ: `normal`, `warm_up`, `drop_set`.

**Rationale**: Warm-up cần loại khỏi volume; drop-set là hiệp ad-hoc (set_number tăng, `rest = 0`) nên `workout_sets.set_type` phải có để đánh dấu khi không có target trước.

**Alternatives**: Enum Java đầy đủ + cột `set_type` riêng (OK); boolean `is_warm_up`/`is_drop_set` (kém mở rộng).

## 5. Superset/giant set/rest-pause?

**Decision**: Defer — cần mô hình nhóm (`group_id`) riêng, nặng, ảnh hưởng UI + snapshot + rest timer.

**Rationale**: YAGNI/KISS; tách feature riêng để migration + UI gọn.

**Alternatives**: Gộp chung (phình scope, rủi ro cao).

## 6. API / migration

**Decision**: Migration mới `V13__advanced_sets.sql` (additive, không sửa migration cũ). Mở rộng payload sửa ngày (`sets[]`) và snapshot/record (`set_type`, `sets[]`); không thêm endpoint mới.

**Rationale**: Constitution §7 — migration immutable, thay đổi phải thêm migration mới; payload hiện có đủ để mở rộng.

**Alternatives**: Endpoint riêng cho target từng hiệp (YAGNI).
