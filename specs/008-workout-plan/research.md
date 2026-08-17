# Research: Thiết lập mục tiêu & Tạo lộ trình tập (008-workout-plan)

**Feature**: specs/008-workout-plan | **Date**: 2026-08-17

Tất cả các điểm NEEDS CLARIFICATION trong Technical Context đã được nghiên cứu và chốt. Mỗi mục gồm Decision / Rationale / Alternatives.

## R1. Kiến trúc Rule Engine v1 — đơn giản hóa

- **Decision**: Triển khai Rule Engine v1 như một **Service thuần** (`RuleEngineService`) chứa hàm Java if/switch theo bảng luật, KHÔNG dùng rule engine framework (Drools, Easy Rules). Input: `(goalType, fitnessLevel, equipment)` → Output: `WorkoutPlan` entity tree (plan → days → exercises).
- **Rationale**: Bảng luật chỉ có 3×3×n combinations — quá nhỏ để cần framework. Code thuần dễ test, dễ maintain, đúng tinh thần "Rule-based đơn giản" mà spec yêu cầu. Không đưa thêm dependency không cần thiết.
- **Alternatives**: Drools (quá nặng cho MVP, thêm XML/YAML rule files); Easy Rules (thêm dependency nhưng không cần thiết cho 3×3 matrix); config YAML riêng (chỉ tách data khỏi code, không giảm complexity).

## R2. Exercise Library — caching & query performance

- **Decision**: Exercise library (1324+ rows) load từ DB mỗi request plan generation, KHÔNG cache in-memory vĩnh viễn. Lý do: exercise có thể bị Admin ẩn/hiện (status change) bất cứ lúc nào; cache 60s là đủ nếu cần优化. Dùng **Spring Data JPA** với `@Query` filter theo `equipment` + `status = 'active'`. Plan generation cần join `exercises` theo category/muscle_group.
- **Rationale**: 1324 rows quá nhỏ để cần Redis/cache phức tạp. JPA query đơn giản đủ nhanh (< 5s cho plan generation — SC-001). Khi cần scale, có thể thêm `@Cacheable` sau.
- **Alternatives**: Redis cache (thêm infrastructure complexity, chưa cần cho MVP); in-memory cache (không sync khi Admin thay đổi); Elasticsearch (quá phức tạp cho 1324 rows).

## R3. Draft Queue — lưu trữ & lifecycle

- **Decision**: Draft Queue được lưu trong bảng `draft_exercises` (PostgreSQL) với `session_id` + `expires_at` (auto-delete khi session hoàn thành). Khi Admin ẩn exercise trong plan active → hệ thống tạo clone row trong `draft_exercises` với `exercise_id` gốc + `session_id` hiện tại. Sau khi workout session hoàn thành → xóa các row `draft_exercises` có `session_id` đó. Khi user thay đổi goal → plan mới, draft queue cũ tự invalidate.
- **Rationale**: Lưu DB thay vì in-memory vì: (1) mobile có thể offline, cần sync draft queue lên server; (2) draft queue cần survive server restart. Lifecycle ngắn (chỉ 1 session) nên không lo retention.
- **Alternatives**: In-memory Map (mất khi restart, không sync mobile); Redis (thêm dependency, draft queue quá ngắn hạn); client-side only (không đồng bộ nếu user đổi thiết bị).

## R4. Goal change — plan archival logic

- **Decision**: Khi user thay đổi `goal_type`: (1) System trả về warning message + 2 options cho client: `immediate` hoặc `fromTomorrow`; (2) Nếu `immediate`: archive plan cũ ngay (status → archived), tạo plan mới active; (3) Nếu `fromTomorrow`: plan cũ giữ active đến hết ngày hôm nay, plan mới active từ 00:00 ngày mai (server timezone). Client gửi request `/api/v1/workout-plans/generate` với `effectiveDate` param.
- **Rationale**: Spec FR-005 yêu cầu 2 lựa chọn rõ ràng. Implementation phía server chỉ cần nhận `effectiveDate` và xử lý accordingly. Client tự decide dựa trên UX.
- **Alternatives**: Server tự decide (immediate vs tomorrow) — vi phạm spec yêu cầu user được chọn; archive không có warning (vi phạm UX spec).

## R5. Exercise search — filter & pagination

- **Decision**: Exercise search API hỗ trợ filter: `equipment`, `category`, `body_part`, `muscle_group`. Phân trang offset-based (page/size). Response include `totalCount` cho UI pagination. KHÔNG filter theo language (instructions mặc định English, Việt ngữ phase sau). Exercise detail endpoint trả full info bao gồm `instructions` JSONB.
- **Rationale**: Spec FR-003 yêu cầu phân loại theo body_part + equipment. FR-006 loại inactive exercises. Search đơn giản cho MVP, có thể mở rộng sau.
- **Alternatives**: Cursor-based pagination (phức tạp hơn, chưa cần cho 1324 rows); filter theo language (chưa có data đa ngôn ngữ).

## R6. Workout Plan — khi nào tạo & trigger

- **Decision**: Plan được tạo tự động SAU KHI user hoàn thành goal setup (FR-002). Flow: (1) User gọi `POST /api/v1/users/me/goals` với goal_type + fitness_level + equipment; (2) Server lưu vào `users` table; (3) Server gọi `RuleEngineService.generatePlan(user)` → tạo plan entities; (4) Return plan response. Plan cũng có thể tạo lại thủ công qua `POST /api/v1/workout-plans/generate` (dùng khi user muốn refresh plan với equipment mới mà không đổi goal).
- **Rationale**: Tách biệt goal setup và plan generation để: (1) goal setup nhanh, plan generation chạy ngay sau; (2) user có thể xem plan trước khi bắt đầu tập; (3) plan generation có thể retry nếu fail.
- **Alternatives**: Tạo plan đồng bộ trong goal setup transaction (goal save + plan generation cùng transaction — nếu plan fail thì goal cũng rollback — không lý tưởng); tạo plan on-demand khi user mở screen workout (thêm latency mỗi lần mở).

## R7. Draft Exercise khi Admin ẩn —替代 suggestion logic

- **Decision**: Khi Admin ẩn exercise trong active plan → system tự tìm exercise thay thế theo: (1) cùng `muscle_group`; (2) cùng `body_part` (movement pattern); (3) `equipment` giống hoặc khác. Query: `SELECT * FROM exercises WHERE muscle_group = ? AND body_part = ? AND status = 'active' AND id != ? ORDER BY RANDOM() LIMIT 1`. Nếu không tìm thấy → trả `replacementSuggestion: null`.
- **Rationale**: Spec FR-011 yêu cầu gợi ý "cùng nhóm cơ, cùng kiểu chuyển động, dụng cụ giống hoặc khác". Query đơn giản dùng 3 filters + random order.
- **Alternatives**: Fuzzy matching (quá phức tạp); gợi ý theo category thay vì muscle_group (kém chính xác hơn).
