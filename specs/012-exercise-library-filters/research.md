# Research: 012 Exercise Library Filters

## R1. Multi-value query params

- **Decision**: `GET /api/v1/exercises` nhận `List<String> category` và `List<String> equipment` (Spring repeated params: `?category=chest&category=back`). Giá trị đơn vẫn hoạt động.
- **Rationale**: Không breaking client cũ gửi một `equipment=body_weight`. JPA `in()` đúng OR trong chiều.
- **Alternatives**: CSV `category=chest,back` (phải parse tay, conflict với giá trị có dấu phẩy — không cần).

## R2. Taxonomy source of truth

- **Decision**: Hardcode 10 category + 28 equipment khớp `exercises-dataset/data/exercises.json` sau khi seeder chuẩn hóa equipment (` ` → `_`). Category giữ khoảng trắng (`upper arms`).
- **Rationale**: Dataset ổn định; endpoint distinct values tốn round-trip không cần cho MVP.
- **Alternatives**: `GET /exercises/facets` — YAGNI.

## R3. Custom form vs library chips

- **Decision**: Thư viện = đa chọn. Form tạo bài = chọn đúng 1 Category, 1 Equipment, 1 nhóm cơ. Lưu `bodyPart = category` (bất biến dataset).
- **Rationale**: Một bài chỉ có một category/equipment; filter đa chọn là chiều tìm kiếm.
- **Alternatives**: Cho custom nhiều category — không khớp entity hiện tại.

## R4. Muscle group chips

- **Decision**: Gỡ khỏi UI thư viện; giữ field trên entity + form custom (6 giá trị).
- **Rationale**: Brainstorm + FR-008/FR-012; Rule Engine v1 vẫn match `muscleGroup`.
- **Alternatives**: Giữ chip 6 nhóm — user đã chọn A (chỉ Category + Equipment).

## R5. Search debounce

- **Decision**: Debounce ~300ms trên ô tên; đổi chip lọc ngay (không nút Submit bắt buộc). Nút tìm kiếm có thể giữ cho a11y/Enter.
- **Rationale**: HTML gốc live-search; SC-003.
